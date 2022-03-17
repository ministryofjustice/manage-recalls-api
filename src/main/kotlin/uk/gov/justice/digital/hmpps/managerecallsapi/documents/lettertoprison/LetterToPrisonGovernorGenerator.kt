package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class LetterToPrisonGovernorGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", context.originalCreatedDate.asStandardDateFormat())

      setVariable("fullName", context.prisonerNameOnLicence)

      with(context.recall) {
        val recallDescription = RecallDescription(this.recallType(), this.recallLength)
        setVariable("recallLengthDescription", recallDescription.asTitle())
        setVariable("recallLengthDays", recallDescription.numberOfDays())
        setVariable("bookingNumber", this.bookingNumber)
        setVariable("lastReleaseDate", this.lastReleaseDate!!.asStandardDateFormat())
      }

      setVariable("currentPrisonName", context.currentPrisonName)
      setVariable("lastReleasePrison", context.lastReleasePrisonName)
    }.let {
      templateEngine.process("letter-to-prison_governor", it)
    }
}
