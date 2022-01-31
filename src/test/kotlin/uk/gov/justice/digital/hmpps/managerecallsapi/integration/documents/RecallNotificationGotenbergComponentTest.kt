package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateLastKnownAddressRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasTotalPageCount
import java.time.LocalDate

class RecallNotificationGotenbergComponentTest : GotenbergComponentTestBase() {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
  private val nomsNumber = NomsNumber("123456")
  private val prisonerFirstName = "Natalia"

  @Test
  fun `can generate a recall notification`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
      currentPrisonId = PrisonId("MWI")
    )

    val recallNotificationId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    val recallNotification = authenticatedClient.getDocument(recall.recallId, recallNotificationId.documentId)

    // writeBase64EncodedStringToFile("recall-notification.pdf", recallNotification.content)
    assertThat(Pdf(recallNotification.content), hasNumberOfPages(equalTo(3)))
    assertThat(Pdf(recallNotification.content), hasTotalPageCount(3))
  }

  @Test
  fun `can generate a not in custody recall notification`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
      currentPrisonId = PrisonId("MWI"),
      inCustody = false
    )
    authenticatedClient.addLastKnownAddress(
      recall.recallId, CreateLastKnownAddressRequest(null, "1 The Road", null, "A Town", "AB12 3CD", AddressSource.MANUAL),
      HttpStatus.CREATED, LastKnownAddressId::class.java
    )

    val recallNotificationId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    val recallNotification = authenticatedClient.getDocument(recall.recallId, recallNotificationId.documentId)

    // writeBase64EncodedStringToFile("recall-notification-nic.pdf", recallNotification.content)
    assertThat(Pdf(recallNotification.content), hasNumberOfPages(equalTo(5)))
    assertThat(Pdf(recallNotification.content), hasTotalPageCount(5))
  }

  @Test
  fun `recall notification with a long recall summary`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      contraband = true,
      contrabandDetail = loremIpsum,
      vulnerabilityDiversity = true,
      vulnerabilityDiversityDetail = loremIpsum,
      LocalDeliveryUnit.ISLE_OF_MAN,
      PrisonId("MWI"),
    )

    val recallNotificationId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    val recallNotification = authenticatedClient.getDocument(recall.recallId, recallNotificationId.documentId)

//    writeBase64EncodedStringToFile("recall-notification-with-long-recall-summary.pdf", recallNotification.content)
    assertThat(Pdf(recallNotification.content), hasNumberOfPages(equalTo(4)))
    assertThat(Pdf(recallNotification.content), hasTotalPageCount(4))
  }
}
