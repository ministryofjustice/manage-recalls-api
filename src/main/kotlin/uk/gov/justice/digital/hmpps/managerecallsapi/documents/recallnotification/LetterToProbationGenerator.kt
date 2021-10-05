package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class LetterToProbationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: LetterToProbationContext): String =
    Context().apply {

      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamContactNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
      setVariable("recallLengthDescription", context.recallLengthDescription.asFixedTermLengthDescription())
      setVariable("probationOfficerName", context.probationOfficerName)
      setVariable("offenderName", context.offenderName)
      setVariable("bookingNumber", context.bookingNumber)
      setVariable("currentPrisonName", context.currentPrisonName)
      setVariable("assessedByUserName", context.assessedByUserName)

      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process("letter-to-probation", it)
    }
}
