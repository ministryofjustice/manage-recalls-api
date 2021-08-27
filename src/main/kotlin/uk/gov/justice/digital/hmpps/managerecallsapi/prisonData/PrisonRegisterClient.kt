package uk.gov.justice.digital.hmpps.managerecallsapi.prisonData

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class PrisonRegisterClient {

  @Autowired
  internal lateinit var prisonRegisterWebClient: WebClient

  fun getAllPrisons(): Mono<List<Prison>> {
    return prisonRegisterWebClient
      .get()
      .uri("/prisons")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Prison>>() {})
  }

  data class Prison(
    val prisonId: String? = null,
    val prisonName: String? = null,
    val active: String? = null
  )
}
