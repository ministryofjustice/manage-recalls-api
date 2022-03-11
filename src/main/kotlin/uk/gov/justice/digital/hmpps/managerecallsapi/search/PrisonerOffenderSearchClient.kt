package uk.gov.justice.digital.hmpps.managerecallsapi.search

import io.micrometer.core.instrument.Counter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeoutException

// FIXME: PUD-1577 - Add some tests to this code

@Component
class PrisonerOffenderSearchClient(
  @Value("\${prisonerSearch.timeout}") val timeout: Long
) {

  @Autowired
  @Qualifier("prisonerOffenderSearchWebClient")
  internal lateinit var webClient: AuthenticatingRestClient

  @Autowired
  @Qualifier("prisonerOffenderSearchTimeoutCounter")
  internal lateinit var timeoutCounter: Counter

  fun prisonerByNomsNumber(nomsNumber: NomsNumber): Mono<Prisoner> =
    webClient
      .get("/prisoner/$nomsNumber")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<Prisoner>() {})
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        Mono.error(ClientException(this.javaClass.simpleName, exception))
      }
      .timeout(Duration.ofSeconds(timeout))
      .onErrorMap(TimeoutException::class.java) { ex ->
        timeoutCounter.increment()
        ClientTimeoutException(this.javaClass.simpleName, ex.javaClass.canonicalName)
      }

  fun prisonerSearch(searchRequest: SearchRequest): Mono<List<Prisoner>> =
    webClient
      .post("/prisoner-search/match-prisoners", PrisonerSearchRequest(searchRequest.nomsNumber))
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Prisoner>>() {})
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        Mono.error(ClientException(this.javaClass.simpleName, exception))
      }
      .timeout(Duration.ofSeconds(timeout))
      .onErrorMap(TimeoutException::class.java) { ex ->
        timeoutCounter.increment()
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

data class PrisonerSearchRequest(val prisonerIdentifier: NomsNumber)
