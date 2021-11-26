package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException
import java.time.OffsetDateTime
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
  @Column(nullable = false)
  val signature: String,
  @Column(nullable = false)
  @Convert(converter = EmailJpaConverter::class)
  val email: Email,
  @Column(nullable = false)
  @Convert(converter = PhoneNumberJpaConverter::class)
  val phoneNumber: PhoneNumber,
  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(userId: UserId, firstName: FirstName, lastName: LastName, signature: String, email: Email, phoneNumber: PhoneNumber, createdDateTime: OffsetDateTime) : this(
    userId.value,
    firstName,
    lastName,
    signature,
    email,
    phoneNumber,
    createdDateTime
  )

  fun userId() = UserId(id)

  fun personName() = PersonName(this.firstName, lastName = this.lastName)

  fun fullName(): String = personName().toString()
}

data class UserDetailsNotFoundException(val userId: UserId) : NotFoundException()

@Repository("jpaUserDetailsRepository")
interface JpaUserDetailsRepository : JpaRepository<UserDetails, UUID>

@NoRepositoryBean
interface ExtendedUserDetailsRepository : JpaUserDetailsRepository {
  fun getByUserId(userId: UserId): UserDetails
  fun findByUserId(userId: UserId): UserDetails?
}

@Component
class UserDetailsRepository(
  @Qualifier("jpaUserDetailsRepository") @Autowired private val jpaRepository: JpaUserDetailsRepository
) : JpaUserDetailsRepository by jpaRepository, ExtendedUserDetailsRepository {
  override fun getByUserId(userId: UserId): UserDetails = findByUserId(userId) ?: throw UserDetailsNotFoundException(userId)

  override fun findByUserId(userId: UserId): UserDetails? = findById(userId.value).orElse(null)
}
