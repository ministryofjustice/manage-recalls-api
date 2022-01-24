package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.LocalDate

@AutoConfigureWebTestClient(timeout = "10000")
abstract class GotenbergComponentTestBase : ComponentTestBase(useRealGotenbergServer = true) {

  protected fun updateRecallWithRequiredInformationForTheDossier(
    recallId: RecallId,
    contraband: Boolean = true,
    contrabandDetail: String = "Lots of naughty contraband\n with a line break",
    vulnerabilityDiversity: Boolean = true,
    vulnerabilityDiversityDetail: String = "Very diverse\n with a line break",
    localDeliveryUnit: LocalDeliveryUnit,
    currentPrisonId: PrisonId,
    inCustody: Boolean = true
  ) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_1,
        previousConvictionMainNameCategory = NameFormatCategory.OTHER,
        previousConvictionMainName = "Nat The Naughty",
        bookingNumber = "NAT0001",
        lastReleasePrison = PrisonId("MWI"),
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(10, 1, 5),
        sentencingCourt = CourtId("ACCRYC"),
        indexOffence = "Some index offence",
        reasonsForRecall = setOf(ReasonForRecall.ELM_FURTHER_OFFENCE),
        probationOfficerName = "Percy Pig",
        probationOfficerPhoneNumber = "0898909090",
        probationOfficerEmail = "probation.officer@moj.com",
        localDeliveryUnit = localDeliveryUnit,
        authorisingAssistantChiefOfficer = "ACO",
        localPoliceForceId = PoliceForceId("metropolitan"),
        currentPrison = currentPrisonId,
        contraband = contraband,
        contrabandDetail = contrabandDetail,
        vulnerabilityDiversity = vulnerabilityDiversity,
        vulnerabilityDiversityDetail = vulnerabilityDiversityDetail,
        licenceConditionsBreached = "licenceConditionsBreached1\nlicenceConditionsBreached2",
        inCustody = inCustody
      )
    )
  }

  protected fun updateRecallWithRequiredInformationForTheLetterToPrison(
    recallId: RecallId,
    contraband: Boolean = true,
    contrabandDetail: String = "Lots of naughty contraband",
    vulnerabilityDiversity: Boolean = true,
    vulnerabilityDiversityDetail: String = "Very diverse",
    assessedByUserId: UserId? = null,
    sentenceYears: Int = 10
  ) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_1,
        previousConvictionMainNameCategory = NameFormatCategory.OTHER,
        previousConvictionMainName = "Nat The Naughty",
        bookingNumber = "NAT0001",
        lastReleasePrison = PrisonId("MWI"),
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(sentenceYears, 1, 5),
        sentencingCourt = CourtId("HVRFCT"),
        indexOffence = "Some index offence",
        reasonsForRecall = setOf(ReasonForRecall.ELM_FURTHER_OFFENCE),
        probationOfficerName = "Percy Pig",
        probationOfficerPhoneNumber = "0898909090",
        probationOfficerEmail = "probation.officer@moj.com",
        localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
        authorisingAssistantChiefOfficer = "ACO",
        localPoliceForceId = PoliceForceId("metropolitan"),
        currentPrison = PrisonId("MWI"),
        contraband = contraband,
        contrabandDetail = contrabandDetail,
        vulnerabilityDiversity = vulnerabilityDiversity,
        vulnerabilityDiversityDetail = vulnerabilityDiversityDetail,
        assessedByUserId = assessedByUserId,
        differentNomsNumber = true,
        differentNomsNumberDetail = "ABC123",
        additionalLicenceConditions = true,
        additionalLicenceConditionsDetail = "Additional licence detail 1\n\nAdditional license detail 2"
      )
    )
  }

  protected fun uploadPartAFor(recall: RecallResponse) {
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(DocumentCategory.PART_A_RECALL_REPORT, base64EncodedFileContents("/document/part_a.pdf"), "PART_A.pdf")
    )
  }

  protected fun uploadLicenceFor(recall: RecallResponse) {
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(DocumentCategory.LICENCE, base64EncodedFileContents("/document/licence.pdf"), "PART_A.pdf")
    )
  }
}
