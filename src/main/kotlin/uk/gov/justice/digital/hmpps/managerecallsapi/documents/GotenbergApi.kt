package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.http.HttpEntity
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClientRequest
import java.time.Duration.ofSeconds

@Component
class GotenbergApi(
  val gotenbergWebClient: WebClient,
) {
  fun merge(mergeRequest: MultiValueMap<String, HttpEntity<*>>): Mono<ByteArray> =
    gotenbergResponse("/forms/pdfengines/merge", mergeRequest)

  fun convertHtml(convertHtmlRequest: MultiValueMap<String, HttpEntity<*>>): Mono<ByteArray> =
    gotenbergResponse("/forms/chromium/convert/html", convertHtmlRequest)

  private fun gotenbergResponse(
    path: String,
    documentBody: MultiValueMap<String, HttpEntity<*>>
  ) = gotenbergWebClient
    .post()
    .uri(path)
    .httpRequest { it.getNativeRequest<HttpClientRequest>().responseTimeout(ofSeconds(10)) }
    .contentType(MULTIPART_FORM_DATA)
    .bodyValue(documentBody)
    .retrieve()
    .bodyToMono(ByteArray::class.java)
}
