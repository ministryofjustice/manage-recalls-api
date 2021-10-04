package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
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

      with(context.prisoner) {
        setVariable("fullName", PersonName(FirstName(this.firstName!!), this.middleNames?.let { MiddleNames(it) }, LastName(this.lastName!!)))
      }

      with(context.recall) {
        setVariable("recallLengthDescription", RecallLengthDescription(this.recallLength!!).asFixedTermLengthDescription())
        setVariable("bookingNumber", this.bookingNumber)
        setVariable("lastReleaseDate", this.lastReleaseDate!!.asStandardDateFormat())
      }

      setVariable("currentEstablishment", context.currentPrisonName)
      setVariable("lastReleasePrison", context.lastReleasePrisonName)
    }.let {
      templateEngine.process("letter-to-prison_governor", it)
    }
}
