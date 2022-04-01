package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseAverageDuration
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecordRepository
import java.time.LocalDate

@Service
class StatisticsService(
  @Autowired private val phaseRecordRepository: PhaseRecordRepository,
) {

  fun getSummary(): StatisticsSummary {
    val lastSevenDaysSummary = phaseRecordRepository.summaryByPhaseSince(LocalDate.now().minusDays(7)).map { it.toPhaseAverageDuration() }
    val overallSummary = phaseRecordRepository.summaryByPhaseSince(LocalDate.MIN).map { it.toPhaseAverageDuration() }
    return StatisticsSummary(lastSevenDaysSummary, overallSummary)
  }
}

data class StatisticsSummary(
  val lastSevenDays: List<Api.PhaseAverageDuration>,
  val overall: List<Api.PhaseAverageDuration>,
)

private fun PhaseAverageDuration.toPhaseAverageDuration() =
  Api.PhaseAverageDuration(phase, averageDuration, count)
