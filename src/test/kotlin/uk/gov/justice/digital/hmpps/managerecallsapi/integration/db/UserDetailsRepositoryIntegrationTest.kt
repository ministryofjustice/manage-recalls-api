package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.io.File
import java.util.Base64
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class UserDetailsRepositoryIntegrationTest(@Autowired private val repository: UserDetailsRepository) {

  private val userId = ::UserId.random()
  private val firstName = FirstName("Bertie")
  private val lastName = LastName("Badger")
  private val signature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())
  private val userDetails = UserDetails(userId, firstName, lastName, signature)

  @Test
  @Transactional
  fun `can save and retrieve UserDetails`() {
    repository.save(userDetails)

    val retrieved = repository.getByUserId(userId)

    assertThat(retrieved, equalTo(userDetails))
  }

  @Test
  fun `get by userId throws UserDetailsNotFoundException if none exist`() {
    assertThrows<UserDetailsNotFoundException> {
      repository.getByUserId(::UserId.random())
    }
  }
}
