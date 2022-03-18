package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_CONTACT_NUMBER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RECALL_TEAM_NAME
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.shouldShowOnDocuments

@Component
class LetterToPrisonCustodyOfficeGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(context: LetterToPrisonCustodyOfficeContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", context.originalCreatedDate.asStandardDateFormat())

      setVariable("fullName", context.prisonerNameOnLicence)

      setVariable("isFixedTermRecall", context.recallDescription.isFixedTermRecall())
      setVariable("recallTitle", context.recallDescription.asTitle())
      setVariable("recallTypeWithoutLength", context.recallDescription.typeWithoutLength())
      if (context.recallDescription.isFixedTermRecall()) {
        setVariable("recallLengthDays", context.recallDescription.numberOfDays())
      }
      setVariable("bookingNumber", context.bookingNumber)
      setVariable("nomsNumberHeldUnder", context.nomsNumberHeldUnder)
      setVariable("differentNomsNumber", context.differentNomsNumber)
      setVariable("originalNomsNumber", context.originalNomsNumber)
      setVariable("hasAdditionalLicenceConditions", context.hasAdditionalLicenceConditions)
      setVariable("additionalLicenceConditionsDetail", context.additionalLicenceConditionsDetail)
      setVariable("hasContraband", context.hasContraband)
      setVariable("contrabandDetail", context.contrabandDetail)
      setVariable("hasVulnerabilities", context.hasVulnerabilities)
      setVariable("vulnerabilityDiversityDetail", context.vulnerabilityDetail)
      setVariable("hasMappaLevel", context.mappaLevel.shouldShowOnDocuments())
      setVariable("mappaLevel", context.mappaLevel.label)

      setVariable("currentEstablishment", context.currentPrisonName)
      setVariable("signatoryName", context.currentUserName)
    }.let {
      templateEngine.process("letter-to-prison_custody-office", it)
    }
}
