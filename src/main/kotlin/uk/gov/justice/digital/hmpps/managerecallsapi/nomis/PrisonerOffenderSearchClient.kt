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
import uk.gov.justice.digital.hmpps.managerecallsapi.config.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.AuthenticatingWebClient
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeoutException

@Component
class PrisonerOffenderSearchClient(
  @Value("\${prisonerSearch.timeout}") val timeout: Long
) {

  @Autowired
  internal lateinit var prisonerOffenderSearchWebClient: AuthenticatingWebClient

  @Autowired
  internal lateinit var prisonerOffenderSearchTimeoutCounter: Counter

  fun prisonerByNomsNumber(nomsNumber: NomsNumber): Mono<Prisoner> =
    prisonerOffenderSearchWebClient
      .get("/prisoner/$nomsNumber")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<Prisoner>() {})
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.error(PrisonerNotFoundException(nomsNumber))
          else -> Mono.error(ClientException(this.javaClass.simpleName, exception))
        }
      }
      .timeout(Duration.ofSeconds(timeout))
      .onErrorMap(TimeoutException::class.java) { ex ->
        prisonerOffenderSearchTimeoutCounter.increment()
        ClientTimeoutException(this.javaClass.simpleName, ex.javaClass.canonicalName)
      }
}

data class Prisoner(
  val prisonerNumber: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val status: String? = null,
  val bookNumber: String? = null
)
