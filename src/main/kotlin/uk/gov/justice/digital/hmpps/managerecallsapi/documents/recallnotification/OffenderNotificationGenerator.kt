package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class OffenderNotificationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: OffenderNotificationContext): String =
    Context().apply {
      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
      setVariable("prisonerNameOnLicense", context.prisonerNameOnLicense)
      setVariable("bookingNumber", context.bookingNumber)
    }.let {
      templateEngine.process("offender-notification", it)
    }
}