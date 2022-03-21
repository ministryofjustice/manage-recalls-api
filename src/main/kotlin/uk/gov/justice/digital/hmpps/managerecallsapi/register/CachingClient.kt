package uk.gov.justice.digital.hmpps.managerecallsapi.register

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import java.time.Duration

abstract class CachingClient<T>(
  val webClient: TimeoutHandlingWebClient
) {

  private val responseCache: Cache<String, T> = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofHours(4))
    .maximumSize(1000)
    .build()

  fun <T> getResponseWith404Handling(uri: String, typeReference: ParameterizedTypeReference<T>): Mono<T> {
    return webClient.getWithTimeout(uri, typeReference, this.javaClass)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.empty()
          else -> Mono.error(ClientException(this.javaClass.simpleName, exception))
        }
      }
  }

  fun getResponse(uri: String, typeReference: ParameterizedTypeReference<T>): Mono<T> {
    return checkCacheElseGetWithTimeout(uri, typeReference)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        Mono.error(ClientException(this.javaClass.simpleName, exception))
      }
  }

  fun clearCache() {
    responseCache.invalidateAll()
  }

  private fun checkCacheElseGetWithTimeout(
    uri: String,
    value: ParameterizedTypeReference<T>
  ): Mono<T> =
    responseCache.getIfPresent(uri)?.let {
      Mono.just(it)
    }
      ?: webClient.getWithTimeout(uri, value, this.javaClass).map {
        responseCache.put(uri, it)
        it
      }
}
