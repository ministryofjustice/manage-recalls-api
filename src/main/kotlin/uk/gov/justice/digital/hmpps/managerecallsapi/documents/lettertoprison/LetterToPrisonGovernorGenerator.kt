package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class LetterToPrisonGovernorGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(context: LetterToPrisonGovernorContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", context.originalCreatedDate.asStandardDateFormat())

      setVariable("fullName", context.prisonerNameOnLicence)

      setVariable("isFixedTermRecall", context.recallDescription.isFixedTermRecall())
      setVariable("recallTitle", context.recallDescription.asTitle())
      if (context.recallDescription.recallType == RecallType.FIXED) {
        setVariable("recallLengthDays", context.recallDescription.numberOfDays())
      }
      setVariable("bookingNumber", context.bookingNumber)
      setVariable("lastReleaseDate", context.lastReleaseDate.asStandardDateFormat())

      setVariable("currentPrisonName", context.currentPrisonName)
      setVariable("lastReleasePrison", context.lastReleasePrisonName)
    }.let {
      templateEngine.process("letter-to-prison_governor", it)
    }
}
