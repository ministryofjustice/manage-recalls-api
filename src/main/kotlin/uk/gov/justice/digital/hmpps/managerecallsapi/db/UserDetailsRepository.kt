package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_details")
data class UserDetails(
  @Id
  val id: UUID,
  @Column(nullable = false)
  @Convert(converter = FirstNameJpaConverter::class)
  val firstName: FirstName,
  @Column(nullable = false)
  @Convert(converter = LastNameJpaConverter::class)
  val lastName: LastName,
) {
  constructor(userId: UserId, firstName: FirstName, lastName: LastName) : this(userId.value, firstName, lastName)

  fun userId() = UserId(id)
}

data class UserDetailsNotFoundException(val userId: UserId) : NotFoundException()

@Repository("jpaUserDetailsRepository")
interface JpaUserDetailsRepository : JpaRepository<UserDetails, UUID>

@NoRepositoryBean
interface ExtendedUserDetailsRepository : JpaUserDetailsRepository {
  fun getByUserId(userId: UserId): UserDetails
}

@Component
class UserDetailsRepository(
  @Qualifier("jpaUserDetailsRepository") @Autowired private val jpaRepository: JpaUserDetailsRepository
) : JpaUserDetailsRepository by jpaRepository, ExtendedUserDetailsRepository {
  override fun getByUserId(userId: UserId): UserDetails =
    try {
      getById(userId.value)
    } catch (e: JpaObjectRetrievalFailureException) {
      throw UserDetailsNotFoundException(userId)
    }
}
