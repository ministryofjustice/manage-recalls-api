package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class LetterToProbationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: LetterToProbationContext): String =
    Context().apply {

      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamContactNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
      if (context.recallType == FIXED) {
        setVariable("recallLengthDescription", context.recallDescription!!.asFixedTermLengthDescription())
      }
      setVariable("probationOfficerName", context.probationOfficerName)
      setVariable("prisonerNameOnLicense", context.prisonerNameOnLicense)
      setVariable("bookingNumber", context.bookingNumber)
      if (context.inCustodyRecall) {
        setVariable("currentPrisonName", context.currentPrisonName!!)
      }
      setVariable("assessedByUserName", context.assessedByUserName)
      setVariable("inCustody", context.inCustodyRecall)

      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process(getTemplateName(context), it)
    }

  private fun getTemplateName(context: LetterToProbationContext) =
    if (context.recallType == STANDARD && context.inCustodyRecall)
      "letter-to-probation_standard_in_custody"
    else "letter-to-probation_others"
}
