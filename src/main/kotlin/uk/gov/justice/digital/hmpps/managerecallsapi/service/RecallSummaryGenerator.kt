package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_1
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_2
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

@Component
class RecallSummaryGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", ENGLISH)
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  fun generateHtml(context: RecallSummaryContext): String =
    Context().apply {
      val createdDate = LocalDate.now(clock).format(dateFormatter)
      val createdTime = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London")).format(timeFormatter)
      setVariable("createdDate", createdDate)
      setVariable("createdTime", createdTime)
      setVariable("recallNotificationTotalNumberOfPages", context.recallNotificationTotalNumberOfPages)

      // TODO: MD What do we do if any of these values are NULL?  They should all be present and valid
      with(context.prisoner) {
        val firstAndMiddleNames = String.format("%s %s", this.firstName ?: "", this.middleNames ?: "").trim()
        setVariable("forename", firstAndMiddleNames)
        setVariable("surname", this.lastName)
        setVariable("dateOfBirth", this.dateOfBirth?.format(dateFormatter))
        setVariable("pncCroNumber", this.croNumber)
      }

      with(context.assessor) {
        setVariable("caseworkerName", "%s %s".format(this.firstName, this.lastName))
        setVariable("caseworkerEmail", this.email)
        setVariable("caseworkerPhoneNumber", this.phoneNumber)
      }

      val recall = context.recall
      with(recall.mappaLevel) {
        setVariable("mappaLevel1", this?.equals(LEVEL_1) ?: false)
        setVariable("mappaLevel2", this?.equals(LEVEL_2) ?: false)
        setVariable("mappaLevel3", this?.equals(LEVEL_3) ?: false)
      }

      with(recall.sentencingInfo) {
        setVariable("lengthOfSentence", this?.sentenceLength?.prettyPrint())
        setVariable("indexOffence", this?.indexOffence)
        setVariable("sentencingCourt", this?.sentencingCourt)
        setVariable("sentencingDate", this?.sentenceDate?.format(dateFormatter))
        setVariable("sed", this?.sentenceExpiryDate?.format(dateFormatter))
      }

      with(recall.probationInfo) {
        setVariable("offenderManagerName", this?.probationOfficerName)
        setVariable("offenderManagerContactNumber", this?.probationOfficerPhoneNumber)
      }

      setVariable("policeFileName", recall.previousConvictionMainName)
      setVariable("prisonNumber", recall.bookingNumber)
      setVariable("pnomisNumber", recall.nomsNumber)
      setVariable("releaseDate", recall.lastReleaseDate?.format(dateFormatter))
      setVariable("furtherCharge", recall.hasFurtherCharge())
      setVariable("policeSpoc", recall.localPoliceForce)
      setVariable("currentPrison", context.currentPrisonName)
      setVariable("vulnerabilityDetail", recall.vulnerabilityDiversityDetail ?: "None")
      setVariable("contraband", if (recall.contrabandDetail.isNullOrEmpty()) "NO" else "YES")
      setVariable("contrabandDetail", recall.contrabandDetail)
      setVariable("releasingPrison", context.lastReleasePrisonName)

      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process("recall-summary", it)
    }

  private fun Recall.hasFurtherCharge() =
    reasonsForRecall.contains(ELM_FURTHER_OFFENCE) || reasonsForRecall.contains(
      POOR_BEHAVIOUR_FURTHER_OFFENCE
    )
}
