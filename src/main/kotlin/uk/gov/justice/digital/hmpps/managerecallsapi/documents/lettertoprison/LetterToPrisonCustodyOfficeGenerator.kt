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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.fullName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.shouldShowOnDocuments
import java.time.Clock
import java.time.LocalDate

@Component
class LetterToPrisonCustodyOfficeGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", LocalDate.now(clock).asStandardDateFormat())

      setVariable("fullName", context.prisoner.fullName())

      with(context.recall) {
        setVariable("recallLengthDescription", RecallLengthDescription(this.recallLength!!).asFixedTermLengthDescription())
        setVariable("bookingNumber", this.bookingNumber)
        setVariable("nomisNumberHeldUnder", if (this.differentNomsNumber!!) this.differentNomsNumberDetail else this.nomsNumber.value)
        setVariable("differentNomsNumber", this.differentNomsNumber)
        setVariable("originalNomisNumber", this.nomsNumber.value)
        setVariable("recallLengthDays", RecallLengthDescription(this.recallLength).numberOfDays())
        setVariable("hasAdditionalLicenceConditions", this.additionalLicenceConditions)
        setVariable("additionalLicenceConditionsDetail", this.additionalLicenceConditionsDetail)
        setVariable("hasVulnerabilities", this.vulnerabilityDiversityDetail?.isNotEmpty())
        setVariable("vulnerabilityDiversityDetail", this.vulnerabilityDiversityDetail)
        setVariable("hasContraband", this.contrabandDetail?.isNotEmpty())
        setVariable("contrabandDetail", this.contrabandDetail)
        setVariable("hasMappaLevel", this.mappaLevel!!.shouldShowOnDocuments())
        setVariable("mappaLevel", this.mappaLevel.label)
      }

      setVariable("currentEstablishment", context.currentPrisonName)
      setVariable("assessorName", context.assessor.fullName())
    }.let {
      templateEngine.process("letter-to-prison_custody-office", it)
    }
}
