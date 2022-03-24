package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

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
class ReturnedToCustodyLetterToProbationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(context: ReturnedToCustodyLetterToProbationContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", context.originalCreatedDate.asStandardDateFormat())

      setVariable("prisonerNameOnLicence", context.prisonerNameOnLicence)
      setVariable("returnedToCustodyDate", context.returnedToCustodyDate.asStandardDateFormat())
      setVariable("probationOfficerName", context.probationOfficerName)
      setVariable("recallTitle", context.recallDescription.asTitle())
      if (context.recallDescription.recallType == FIXED) {
        setVariable("recallLengthDays", context.recallDescription.numberOfDays())
      } else {
        setVariable("partBDueDate", context.partBDueDate!!)
        setVariable("authorisingAssistantChiefOfficer", context.authorisingAssistantChiefOfficer)
      }
      setVariable("bookingNumber", context.bookingNumber)
      setVariable("nomsNumberHeldUnder", context.nomsNumberHeldUnder)
      setVariable("differentNomsNumber", context.differentNomsNumber)
      setVariable("originalNomsNumber", context.originalNomsNumber)

      setVariable("currentPrisonName", context.currentPrisonName)
    }.let {
      templateEngine.process(getTemplateName(context), it)
    }

  private fun getTemplateName(context: ReturnedToCustodyLetterToProbationContext) =
    when (context.recallDescription.recallType) {
      STANDARD -> "returned-to-custody_letter-to-probation_standard"
      FIXED -> "returned-to-custody_letter-to-probation_fixed"
    }
}
