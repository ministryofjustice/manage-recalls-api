package uk.gov.justice.digital.hmpps.managerecallsapi.prisonData

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

@Component
class PrisonRegisterClient(
  @Autowired private val prisonRegisterWebClient: WebClient
) {

  fun getAllPrisons(): Mono<List<Prison>> {
    return prisonRegisterWebClient
      .get()
      .uri("/prisons")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Prison>>() {})
  }

}

data class Prison(val prisonId: PrisonId, val prisonName: PrisonName, val active: Boolean)