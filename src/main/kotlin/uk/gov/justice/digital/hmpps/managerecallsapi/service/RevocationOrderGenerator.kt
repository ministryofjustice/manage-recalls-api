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

  fun generateHtml(revocationOrderContext: RevocationOrderContext): String =
    Context().apply {
      setVariable("firstNames", revocationOrderContext.firstAndMiddleNames)
      setVariable("lastName", revocationOrderContext.lastName)
      setVariable("dateOfBirth", revocationOrderContext.dateOfBirth)
      setVariable("prisonNumber", revocationOrderContext.bookingNumber)
      setVariable("croNumber", revocationOrderContext.croNumber)
      setVariable("licenseRevocationDate", revocationOrderContext.today.asStandardDateFormat())
      setVariable("lastReleaseDate", revocationOrderContext.lastReleaseDate.asStandardDateFormat())
      setVariable("logoFileName", RevocationOrderLogo.fileName)
    }.let {
      templateEngine.process("revocation-order", it)
    }
}
