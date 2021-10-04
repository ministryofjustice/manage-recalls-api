package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_1
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_2
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Component
class RecallSummaryGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(recallSummaryContext: RecallSummaryContext, recallNotificationTotalNumberOfPages: Int?): String =
    Context().apply {
      setVariable("createdDate", recallSummaryContext.createdDateTime.toLocalDate().asStandardDateFormat())
      setVariable("createdTime", recallSummaryContext.createdDateTime.asStandardTimeFormat())
      setVariable("recallNotificationTotalNumberOfPages", recallNotificationTotalNumberOfPages)

      setVariable("forename", recallSummaryContext.firstAndMiddleNames)
      setVariable("surname", recallSummaryContext.lastName)
      setVariable("dateOfBirth", recallSummaryContext.dateOfBirth.asStandardDateFormat())
      setVariable("pncCroNumber", recallSummaryContext.croNumber)

      setVariable("caseworkerName", recallSummaryContext.assessedByUserName)
      setVariable("caseworkerEmail", recallSummaryContext.assessedByUserEmail)
      setVariable("caseworkerPhoneNumber", recallSummaryContext.assessedByUserPhoneNumber)

      setVariable("mappaLevel1", recallSummaryContext.mappaLevel == LEVEL_1)
      setVariable("mappaLevel2", recallSummaryContext.mappaLevel == LEVEL_2)
      setVariable("mappaLevel3", recallSummaryContext.mappaLevel == LEVEL_3)

      setVariable("lengthOfSentence", recallSummaryContext.lengthOfSentence)
      setVariable("indexOffence", recallSummaryContext.indexOffence)
      setVariable("sentencingCourt", recallSummaryContext.sentencingCourt)
      setVariable("sentencingDate", recallSummaryContext.sentenceDate.asStandardDateFormat())
      setVariable("sed", recallSummaryContext.sentenceExpiryDate.asStandardDateFormat())

      setVariable("offenderManagerName", recallSummaryContext.probationOfficerName)
      setVariable("offenderManagerContactNumber", recallSummaryContext.probationOfficerPhoneNumber)
      setVariable("localDeliveryUnit", recallSummaryContext.localDeliveryUnit.label)

      setVariable("policeFileName", recallSummaryContext.previousConvictionMainName)
      setVariable("prisonNumber", recallSummaryContext.bookingNumber)
      setVariable("pnomisNumber", recallSummaryContext.nomsNumber)
      setVariable("releaseDate", recallSummaryContext.lastReleaseDate.asStandardDateFormat())
      setVariable("furtherCharge", recallSummaryContext.reasonsForRecall.isFurtherCharge())
      setVariable("policeSpoc", recallSummaryContext.localPoliceForce)
      setVariable("vulnerabilityDetail", ValueOrNone(recallSummaryContext.vulnerabilityDiversityDetail))
      setVariable("contraband", YesOrNo(recallSummaryContext.contrabandDetail.hasContrabandDetail()))
      setVariable("contrabandDetail", recallSummaryContext.contrabandDetail)

      setVariable("currentPrison", recallSummaryContext.currentPrisonName)
      setVariable("releasingPrison", recallSummaryContext.lastReleasePrisonName)

      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process("recall-summary", it)
    }

  private fun Set<ReasonForRecall>.isFurtherCharge() =
    this.contains(ELM_FURTHER_OFFENCE) || this.contains(POOR_BEHAVIOUR_FURTHER_OFFENCE)

  private fun String?.hasContrabandDetail(): Boolean = this.isNullOrEmpty().not()
}
