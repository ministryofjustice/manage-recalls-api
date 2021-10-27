package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.personName
import java.time.Clock
import java.time.LocalDate

@Component
class LetterToPrisonGovernorGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", LocalDate.now(clock).asStandardDateFormat())

      setVariable("fullName", context.prisoner.personName())

      with(context.recall) {
        val recallLengthDescription = RecallLengthDescription(this.recallLength!!)
        setVariable("recallLengthDescription", recallLengthDescription.asFixedTermLengthDescription())
        setVariable("recallLengthDays", recallLengthDescription.numberOfDays())
        setVariable("bookingNumber", this.bookingNumber)
        setVariable("lastReleaseDate", this.lastReleaseDate!!.asStandardDateFormat())
      }

      setVariable("currentPrisonName", context.currentPrisonName)
      setVariable("lastReleasePrison", context.lastReleasePrisonName)
    }.let {
      templateEngine.process("letter-to-prison_governor", it)
    }
}
