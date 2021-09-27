package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate

@ActiveProfiles("db-test")
class DossierGenerationGotenbergComponentTest : ComponentTestBase(startGotenbergMockServer = false) {

  private val nomsNumber = NomsNumber("123456")
  private val prisonerFirstName = "Natalia"
  private val assessedByUserId = ::UserId.random()

  @Test
  fun `can generate a recall notification using gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallSummary(recall)

    val recallNotification = authenticatedClient.getRecallNotification(recall.recallId, assessedByUserId)

//    writeBase64EncodedStringToFile("recall-notification.pdf", recallNotification.content)
    assertThat(recallNotification, hasNumberOfPages(equalTo(3)))
  }

  @Test
  fun `can generate a dossier using gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallSummary(recall)
    authenticatedClient.getRecallNotification(recall.recallId, assessedByUserId)
    uploadLicenceFor(recall)
    uploadPartAFor(recall)

    val dossier = authenticatedClient.getDossier(recall.recallId)

//    writeBase64EncodedStringToFile("dossier.pdf", dossier.content)
    assertThat(dossier, hasNumberOfPages(equalTo(9)))
  }

  private fun setupUserDetailsFor(userId: UserId) {
    userDetailsRepository.save(
      UserDetails(
        userId, FirstName("Bertie"), LastName("Badger"),
        base64EncodedFileContents("/signature.jpg")
      )
    )
  }

  private fun updateRecallWithRequiredInformationForTheRecallSummary(recall: RecallResponse) {
    authenticatedClient.updateRecall(
      recall.recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_1,
        previousConvictionMainName = "Nat The Naughty",
        bookingNumber = "NAT0001",
        lastReleasePrison = "MWI",
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(10, 1, 5),
        sentencingCourt = "Badger court",
        indexOffence = "Some index offence",
        reasonsForRecall = setOf(ReasonForRecall.ELM_FURTHER_OFFENCE),
        probationOfficerName = "Percy Pig",
        probationOfficerPhoneNumber = "0898909090",
        probationOfficerEmail = "probation.officer@moj.com",
        localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
        authorisingAssistantChiefOfficer = "ACO",
        localPoliceForce = "London",
        currentPrison = "MWI",
        vulnerabilityDiversityDetail = "Very diverse",
        contrabandDetail = "Lots of naughty contraband"
      )
    )
  }

  private fun uploadPartAFor(recall: RecallResponse) {
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(PART_A_RECALL_REPORT, base64EncodedFileContents("/document/part_a.pdf"))
    )
  }

  private fun uploadLicenceFor(recall: RecallResponse) {
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(LICENCE, base64EncodedFileContents("/document/licence.pdf"))
    )
  }

  private fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, prisonerFirstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = prisonerFirstName,
          lastName = "Oskina",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )
  }
}
