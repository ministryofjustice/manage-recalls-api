package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasTotalPageCount

class RecallNotificationGotenbergComponentTest : GotenbergComponentTestBase() {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
  private val nomsNumber = NomsNumber("123456")
  private val prisonerFirstName = "Natalia"

  @Test
  fun `can generate a recall notification using gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger")))
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
      currentPrisonId = PrisonId("MWI")
    )

    val recallNotification = authenticatedClient.getRecallNotification(recall.recallId)

//    writeBase64EncodedStringToFile("recall-notification.pdf", recallNotification.content)
    assertThat(recallNotification, hasNumberOfPages(equalTo(3)))
    assertThat(recallNotification, hasTotalPageCount(3))
  }

  @Test
  fun `recall notification with a long recall summary`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger")))
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      contraband = true,
      contrabandDetail = loremIpsum,
      vulnerabilityDiversity = true,
      vulnerabilityDiversityDetail = loremIpsum,
      LocalDeliveryUnit.ISLE_OF_MAN,
      PrisonId("MWI"),
    )

    val recallNotification = authenticatedClient.getRecallNotification(recall.recallId)

//    writeBase64EncodedStringToFile("recall-notification-with-long-recall-summary.pdf", recallNotification.content)
    assertThat(recallNotification, hasNumberOfPages(equalTo(4)))
    assertThat(recallNotification, hasTotalPageCount(4))
  }
}
