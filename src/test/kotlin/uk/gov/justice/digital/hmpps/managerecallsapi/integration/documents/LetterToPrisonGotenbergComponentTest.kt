package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages

class LetterToPrisonGotenbergComponentTest : GotenbergComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val prisonerFirstName = "Natalia"
  private val assessedByUserId = ::UserId.random()
  private val recallNotificationUserId = ::UserId.random()

  @Test
  fun `can generate a letter to prison using gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)
    setupUserDetailsFor(assessedByUserId)
    setupUserDetailsFor(recallNotificationUserId)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallSummary(recall.recallId)

    val letterToPrison = authenticatedClient.getLetterToPrison(recall.recallId)
    writeBase64EncodedStringToFile("letter-to-prison.pdf", letterToPrison.content)
    assertThat(letterToPrison, hasNumberOfPages(equalTo(4)))
  }
}
