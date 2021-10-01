package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import java.time.Clock
import java.time.LocalDate

@Component
class LetterToPrisonCustodyOfficeGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      setVariable("logoFileName", RecallImage.HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", LocalDate.now(clock).asStandardDateFormat())
      with(context.prisoner) {
        setVariable("fullName", PersonName(FirstName(this.firstName!!), this.middleNames?.let { MiddleNames(it) }, LastName(this.lastName!!)))
      }

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
        setVariable("mappaLevel", this.mappaLevel)
      }

      setVariable("currentEstablishment", context.currentPrisonName)
      setVariable("assessorName", context.assessor.fullName())

//      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
    }.let {
      templateEngine.process("letter-to-prison_custody-office", it)
    }
}
