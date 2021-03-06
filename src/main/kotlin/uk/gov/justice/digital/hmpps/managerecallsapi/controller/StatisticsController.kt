package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.service.StatisticsService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.StatisticsSummary

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class StatisticsController(
  @Autowired private val statisticsService: StatisticsService,
) {

  @GetMapping("/statistics/summary")
  @Operation(summary = "Returns a summary of statistics for the recall service")
  fun summary(): StatisticsSummary =
    statisticsService.getSummary()
}
