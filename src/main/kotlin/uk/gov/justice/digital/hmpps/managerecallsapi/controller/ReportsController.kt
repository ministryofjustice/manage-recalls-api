package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReportsService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReportsService.Api.GetReportResponse
import java.time.Clock
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class ReportsController(
  @Autowired private val reportsService: ReportsService,
  @Autowired private val clock: Clock
) {

  @GetMapping("/reports/weekly-recalls-new")
  @Operation(summary = "WIP: Returns a CSV of new weekly recalls information")
  fun weeklyRecallsNew(): GetReportResponse {
    val now = OffsetDateTime.now(clock)
    return reportsService.getWeeklyRecallsNew(now.minusDays(7))
  }
}
