package uk.gov.justice.digital.hmpps.managerecallsapi.nomis

import io.micrometer.core.instrument.Counter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.AuthenticatingWebClient
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeoutException

@Component
class PrisonApiClient(
  @Value("\${clientApi.timeout}") val timeout: Long
) {

  @Autowired
  internal lateinit var prisonApiWebClient: AuthenticatingWebClient

  @Autowired
  internal lateinit var prisonApiTimeoutCounter: Counter

  fun latestInboundMovements(nomsNumbers: Set<NomsNumber>): List<Movement> =
    prisonApiWebClient
      .post("/api/movements/offenders/?latestOnly=true&movementTypes=ADM", nomsNumbers)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Movement>>() {})
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        Mono.error(ClientException(this.javaClass.simpleName, exception))
      }
      .timeout(Duration.ofSeconds(timeout))
      .onErrorMap(TimeoutException::class.java) { ex ->
        prisonApiTimeoutCounter.increment()
        ClientTimeoutException(this.javaClass.simpleName, ex.javaClass.canonicalName)
      }
      .block()!!
}

data class Movement(
  val offenderNo: String,
  val movementDate: LocalDate,
  val movementTime: LocalTime,
) {
  fun nomsNumber() = NomsNumber(offenderNo)
  fun movementDateTime(): OffsetDateTime = OffsetDateTime.of(movementDate, movementTime, ZoneOffset.UTC)
}
