package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PdfDocumentGenerator(
  private val webClient: WebClient,
  @Value("\${gotenberg.base-url}") private val gotenbergBaseUrl: String
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun makePdf(): ByteArray {
    val bodyBuilder = MultipartBodyBuilder()

    bodyBuilder.part("files", ClassPathResource("/document/template/revocation-order/index.html").file.readBytes())
    bodyBuilder.part("files", ClassPathResource("/document/template/revocation-order/logo.png").file.readBytes())

    return webClient
      .post()
      .uri("$gotenbergBaseUrl/convert/html")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .bodyValue(bodyBuilder.build())
      .retrieve()
      .bodyToMono(ByteArray::class.java)
      .block()!!
  }
}
