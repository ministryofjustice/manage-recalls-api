package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController() {
  @GetMapping("/health")
  fun getHealth(): ResponseEntity<String> = ResponseEntity.ok("OK")
}
