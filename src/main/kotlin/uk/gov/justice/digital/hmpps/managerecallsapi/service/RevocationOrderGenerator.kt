package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo

@Component
class RevocationOrderGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: RevocationOrderContext): String =
    Context().apply {
      setVariable("firstNames", context.firstAndMiddleNames)
      setVariable("lastName", context.lastName)
      setVariable("dateOfBirth", context.dateOfBirth)
      setVariable("prisonNumber", context.bookingNumber)
      setVariable("croNumber", context.croNumber)
      setVariable("licenseRevocationDate", context.today)
      setVariable("lastReleaseDate", context.lastReleaseDate)
      setVariable("logoFileName", RevocationOrderLogo.fileName)
    }.let {
      templateEngine.process("revocation-order", it)
    }
}
