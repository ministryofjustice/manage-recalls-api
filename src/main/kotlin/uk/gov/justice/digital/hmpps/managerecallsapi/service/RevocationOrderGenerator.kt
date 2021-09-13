package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RevocationOrderGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(prisoner: Prisoner): String =
    Context().apply {
      val firstAndMiddleNames = String.format("%s %s", prisoner.firstName, prisoner.middleNames).trim()
      val todaysDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
      setVariable("firstNames", firstAndMiddleNames)
      setVariable("lastName", prisoner.lastName)
      setVariable("dateOfBirth", prisoner.dateOfBirth)
      setVariable("prisonNumber", prisoner.bookNumber)
      setVariable("croNumber", prisoner.croNumber)
      setVariable("licenseRevocationDate", todaysDate)
    }.let {
      templateEngine.process("revocation-order", it)
    }
}
