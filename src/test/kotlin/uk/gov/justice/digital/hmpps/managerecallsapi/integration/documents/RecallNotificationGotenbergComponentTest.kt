package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages

class RecallNotificationGotenbergComponentTest : GotenbergComponentTestBase() {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
  private val nomsNumber = NomsNumber("123456")
  private val prisonerFirstName = "Natalia"
  private val assessedByUserId = ::UserId.random()

  @Test
  fun `can generate a recall notification using gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallSummary(recall.recallId)

    val recallNotification = authenticatedClient.getRecallNotification(recall.recallId, assessedByUserId)

//    writeBase64EncodedStringToFile("recall-notification.pdf", recallNotification.content)
    assertThat(recallNotification, hasNumberOfPages(equalTo(3)))
  }

  @Test
  fun `recall notification with a long recall summary`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallSummary(
      recall.recallId,
      vulnerabilityDiversityDetail = loremIpsum,
      contrabandDetail = loremIpsum
    )

    val recallNotification = authenticatedClient.getRecallNotification(recall.recallId, assessedByUserId)

    writeBase64EncodedStringToFile("recall-notification-long-summary.pdf", recallNotification.content)
    assertThat(recallNotification, hasNumberOfPages(equalTo(4)))
  }
}
