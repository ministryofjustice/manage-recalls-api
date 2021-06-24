package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

abstract class PingHealthCheck(
  private val webClient: WebClient,
  private val healthUrl: String,
  private val timeout: Duration = Duration.ofSeconds(2)
) : HealthIndicator {

  override fun health(): Health? {
    return webClient.get()
      .uri(healthUrl)
      .retrieve()
      .toEntity(String::class.java)
      .flatMap { upWithStatus(it) }
      .onErrorResume(WebClientResponseException::class.java) { downWithResponseBody(it) }
      .onErrorResume(Exception::class.java) { downWithException(it) }
      .block(timeout)
  }

  private fun downWithException(it: Exception) = Mono.just(Health.down(it).build())

  private fun downWithResponseBody(it: WebClientResponseException) =
    Mono.just(
      Health.down(it).withBody(it.responseBodyAsString).withHttpStatus(it.statusCode).build()
    )

  private fun upWithStatus(it: ResponseEntity<String>): Mono<Health> =
    Mono.just(Health.up().withHttpStatus(it.statusCode).build())

  private fun Health.Builder.withHttpStatus(status: HttpStatus) = this.withDetail("status", status)

  private fun Health.Builder.withBody(body: String) = this.withDetail("body", body)
}
