package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ValueOrNone
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.YesOrNo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardTimeFormat

@Component
class RecallSummaryGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(recallSummaryContext: RecallSummaryContext, recallNotificationTotalNumberOfPages: Int?): String =
    Context().apply {
      setVariable("createdDate", recallSummaryContext.originalCreatedDateTime.toLocalDate().asStandardDateFormat())
      setVariable("createdTime", recallSummaryContext.originalCreatedDateTime.asStandardTimeFormat())
      setVariable("recallNotificationTotalNumberOfPages", recallNotificationTotalNumberOfPages)

      setVariable("prisonerNameOnLicence", recallSummaryContext.prisonerNameOnLicence)
      setVariable("dateOfBirth", recallSummaryContext.dateOfBirth.asStandardDateFormat())
      setVariable("croNumber", recallSummaryContext.croNumber)

      setVariable("assessedByUserName", recallSummaryContext.assessedByUserName)
      setVariable("assessedByUserEmail", recallSummaryContext.assessedByUserEmail)
      setVariable("assessedByUserPhoneNumber", recallSummaryContext.assessedByUserPhoneNumber)

      setVariable("mappaLevel1", recallSummaryContext.mappaLevel == LEVEL_1)
      setVariable("mappaLevel2", recallSummaryContext.mappaLevel == LEVEL_2)
      setVariable("mappaLevel3", recallSummaryContext.mappaLevel == LEVEL_3)

      setVariable("lengthOfSentence", recallSummaryContext.lengthOfSentence)
      setVariable("indexOffence", recallSummaryContext.indexOffence)
      setVariable("sentencingCourt", recallSummaryContext.sentencingCourt)
      setVariable("sentencingDate", recallSummaryContext.sentenceDate.asStandardDateFormat())
      setVariable("sentenceExpiryDate", recallSummaryContext.sentenceExpiryDate.asStandardDateFormat())

      setVariable("probationOfficerName", recallSummaryContext.probationOfficerName)
      setVariable("probationOfficerPhoneNumber", recallSummaryContext.probationOfficerPhoneNumber)
      setVariable("localDeliveryUnit", recallSummaryContext.localDeliveryUnit.label)

      setVariable("previousConvictionMainName", recallSummaryContext.previousConvictionMainName)
      setVariable("bookingNumber", recallSummaryContext.bookingNumber)
      setVariable("nomsNumber", recallSummaryContext.nomsNumber)
      setVariable("lastReleaseDate", recallSummaryContext.lastReleaseDate.asStandardDateFormat())
      setVariable("furtherCharge", recallSummaryContext.reasonsForRecall.isFurtherCharge())
      setVariable("localPoliceForce", recallSummaryContext.localPoliceForceName)
      setVariable("hasContrabandDetail", YesOrNo(recallSummaryContext.contraband))
      setVariable("contrabandDetail", recallSummaryContext.contrabandDetail)
      setVariable("vulnerabilityDiversityDetail", ValueOrNone(recallSummaryContext.vulnerabilityDiversityDetail))

      if (recallSummaryContext.inCustodyRecall) {
        setVariable("currentPrisonName", recallSummaryContext.currentPrisonName!!)
      } else {
        setVariable("lastKnownAddress", recallSummaryContext.lastKnownAddress!!)
      }
      setVariable("lastReleasePrisonName", recallSummaryContext.lastReleasePrisonName)

      setVariable("inCustody", recallSummaryContext.inCustodyRecall)
      setVariable("arrestIssues", recallSummaryContext.arrestIssues)
      setVariable("arrestIssuesDetail", recallSummaryContext.arrestIssuesDetail)

      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process("recall-summary", it)
    }

  private fun Set<ReasonForRecall>.isFurtherCharge() =
    this.contains(ELM_FURTHER_OFFENCE) || this.contains(POOR_BEHAVIOUR_FURTHER_OFFENCE)
}
