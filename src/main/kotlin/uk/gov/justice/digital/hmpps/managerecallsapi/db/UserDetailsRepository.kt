package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException
import java.util.UUID

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
