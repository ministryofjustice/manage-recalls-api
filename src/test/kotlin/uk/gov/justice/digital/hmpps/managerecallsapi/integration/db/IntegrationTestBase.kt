package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddressRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.OffsetDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
@TestInstance(PER_CLASS)
abstract class IntegrationTestBase {
  final val recallId = ::RecallId.random()
  final val createdByUserId = ::UserId.random()
  val currentUserId = ::UserId.random()
  final val nomsNumber = randomNoms()
  val recall = Recall(recallId, nomsNumber, createdByUserId, OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"))

  @Autowired
  protected lateinit var userDetailsRepository: UserDetailsRepository

  @Autowired
  protected lateinit var recallRepository: RecallRepository

  @Autowired
  protected lateinit var lastKnownAddressRepository: LastKnownAddressRepository

  @BeforeEach
  fun `setup createdBy user`() {
    createUserDetails(createdByUserId)
    createUserDetails(currentUserId)
  }

  private fun createUserDetails(userId: UserId) {
    userDetailsRepository.save(
      UserDetails(
        userId,
        FirstName("Test"),
        LastName("User"),
        "",
        Email("test@user.com"),
        PhoneNumber("09876543210"),
        CaseworkerBand.FOUR_PLUS,
        OffsetDateTime.now()
      )
    )
  }
}
