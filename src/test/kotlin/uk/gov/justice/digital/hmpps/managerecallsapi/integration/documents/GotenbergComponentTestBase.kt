package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate

@AutoConfigureWebTestClient(timeout = "10000")
abstract class GotenbergComponentTestBase : ComponentTestBase(useRealGotenbergServer = true) {

  protected fun updateRecallWithRequiredInformationForTheDossier(
    recallId: RecallId,
    contraband: Boolean = true,
    contrabandDetail: String = "Lots of naughty contraband",
    vulnerabilityDiversity: Boolean = true,
    vulnerabilityDiversityDetail: String = "Very diverse",
  ) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_1,
        previousConvictionMainName = "Nat The Naughty",
        bookingNumber = "NAT0001",
        lastReleasePrison = PrisonId("MWI"),
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
        currentPrison = PrisonId("MWI"),
        contraband = contraband,
        contrabandDetail = contrabandDetail,
        vulnerabilityDiversity = vulnerabilityDiversity,
        vulnerabilityDiversityDetail = vulnerabilityDiversityDetail,
        licenceConditionsBreached = "licenceConditionsBreached"
      )
    )
  }

  protected fun updateRecallWithRequiredInformationForTheLetterToPrison(
    recallId: RecallId,
    contraband: Boolean = true,
    contrabandDetail: String = "Lots of naughty contraband",
    vulnerabilityDiversity: Boolean = true,
    vulnerabilityDiversityDetail: String = "Very diverse",
    assessedByUserId: UserId? = null
  ) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_1,
        previousConvictionMainName = "Nat The Naughty",
        bookingNumber = "NAT0001",
        lastReleasePrison = PrisonId("MWI"),
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
        currentPrison = PrisonId("MWI"),
        contraband = contraband,
        contrabandDetail = contrabandDetail,
        vulnerabilityDiversity = vulnerabilityDiversity,
        vulnerabilityDiversityDetail = vulnerabilityDiversityDetail,
        assessedByUserId = assessedByUserId,
        differentNomsNumber = true,
        differentNomsNumberDetail = "ABC123"
      )
    )
  }

  protected fun uploadPartAFor(recall: RecallResponse) {
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(RecallDocumentCategory.PART_A_RECALL_REPORT, base64EncodedFileContents("/document/part_a.pdf"))
    )
  }

  protected fun uploadLicenceFor(recall: RecallResponse) {
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(RecallDocumentCategory.LICENCE, base64EncodedFileContents("/document/licence.pdf"))
    )
  }

  protected fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, prisonerFirstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = prisonerFirstName,
          lastName = "Badger",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )
  }
}
