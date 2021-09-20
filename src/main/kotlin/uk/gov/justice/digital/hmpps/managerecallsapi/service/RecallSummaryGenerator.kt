package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class RecallSummaryGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  fun generateHtml(context: RecallSummaryContext): String =
    Context().apply {
      val createdDate = LocalDate.now(clock).format(dateFormatter)
      val createdTime = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London")).format(timeFormatter)
      val firstAndMiddleNames = String.format("%s %s", context.prisoner.firstName ?: "", context.prisoner.middleNames ?: "").trim()
      setVariable("createdDate", createdDate)
      setVariable("createdTime", createdTime)

      setVariable("mappaLevel1", context.recall.mappaLevel?.equals(MappaLevel.LEVEL_1) ?: false)
      setVariable("mappaLevel2", context.recall.mappaLevel?.equals(MappaLevel.LEVEL_2) ?: false)
      setVariable("mappaLevel3", context.recall.mappaLevel?.equals(MappaLevel.LEVEL_3) ?: false)

      setVariable("forename", firstAndMiddleNames)
      setVariable("surname", context.prisoner.lastName)
      setVariable("dateOfBirth", context.prisoner.dateOfBirth?.format(dateFormatter))
      setVariable("policeFileName", context.recall.previousConvictionMainName)
      setVariable("prisonNumber", context.recall.bookingNumber)
      setVariable("pnomisNumber", context.recall.nomsNumber)
      setVariable("releasingPrison", context.lastReleasePrisonName)
      setVariable("releaseDate", context.recall.lastReleaseDate?.format(dateFormatter))
      setVariable("lengthOfSentence", context.recall.sentencingInfo?.sentenceLength?.prettyPrint())
      setVariable("indexOffence", context.recall.sentencingInfo?.indexOffence)
      setVariable("furtherCharge", context.recall.reasonsForRecall.contains(ReasonForRecall.ELM_FURTHER_OFFENCE) || context.recall.reasonsForRecall.contains(ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE))

      setVariable("pncCroNumber", context.prisoner.croNumber)
      setVariable("offenderManagerName", context.recall.probationInfo?.probationOfficerName)
      setVariable("offenderManagerContactNumber", context.recall.probationInfo?.probationOfficerPhoneNumber)
      setVariable("policeSpoc", context.recall.localPoliceForce)
      setVariable("currentPrison", context.currentPrisonName)
      setVariable("sentencingCourt", context.recall.sentencingInfo?.sentencingCourt)
      setVariable("sentencingDate", context.recall.sentencingInfo?.sentenceDate?.format(dateFormatter))
      setVariable("sed", context.recall.sentencingInfo?.sentenceExpiryDate?.format(dateFormatter))
      setVariable("vulnerabilityDetail", context.recall.vulnerabilityDiversityDetail ?: "None")
      setVariable("contraband", if (context.recall.contrabandDetail.isNullOrEmpty()) "NO" else "YES")
      setVariable("contrabandDetail", context.recall.contrabandDetail)
    }.let {
      templateEngine.process("recall-summary", it)
    }
}
