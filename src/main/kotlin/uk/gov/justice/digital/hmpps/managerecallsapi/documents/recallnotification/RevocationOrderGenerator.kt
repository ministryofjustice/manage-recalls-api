package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class RevocationOrderGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(revocationOrderContext: RevocationOrderContext): String =
    Context().apply {
      setVariable("fullName", revocationOrderContext.personName.firstAndLastName())
      setVariable("dateOfBirth", revocationOrderContext.dateOfBirth.asStandardDateFormat())
      setVariable("bookingNumber", revocationOrderContext.bookingNumber)
      setVariable("croNumber", revocationOrderContext.croNumber)
      setVariable("licenseRevocationDate", revocationOrderContext.licenseRevocationDate.asStandardDateFormat())
      setVariable("lastReleaseDate", revocationOrderContext.lastReleaseDate.asStandardDateFormat())
      setVariable("logoFileName", RevocationOrderLogo.fileName)
    }.let {
      templateEngine.process("revocation-order", it)
    }
}
