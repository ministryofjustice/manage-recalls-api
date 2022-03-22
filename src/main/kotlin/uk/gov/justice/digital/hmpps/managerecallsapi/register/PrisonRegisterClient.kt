package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.CachingClient
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.TimeoutHandlingWebClient

@Component
class PrisonRegisterClient(
  @Autowired private val prisonRegisterWebClient: TimeoutHandlingWebClient,
) : CachingClient<List<Api.Prison>>(prisonRegisterWebClient) {

  fun getAllPrisons(): Mono<List<Api.Prison>> =
    getResponse("/prisons", object : ParameterizedTypeReference<List<Api.Prison>>() {})

  fun findPrisonById(prisonId: PrisonId): Mono<Api.Prison> =
    getAllPrisons().mapNotNull { list ->
      list.firstOrNull { it.prisonId == prisonId }
    }
}
