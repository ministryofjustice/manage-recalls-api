package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class HealthController() {
  @GetMapping("/health")
  @ResponseStatus(HttpStatus.OK)
  fun getHealth(): Map<String, String> =
    hashMapOf(
      "status" to "UP",
      "version" to version(),
      "buildUrl" to buildUrl()
    )

  private

  fun version(): String = System.getenv("BUILD_NUMBER") ?: "app_version"

  fun buildUrl(): String = System.getenv("BUILD_URL") ?: "https://example.com"
}
