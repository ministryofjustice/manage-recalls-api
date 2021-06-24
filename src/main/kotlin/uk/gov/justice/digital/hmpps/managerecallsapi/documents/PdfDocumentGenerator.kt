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
  @Value("\${gotenberg.endpoint.url}") private val gotenbergBaseUrl: String
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun makePdf(): ByteArray {
    val bodyBuilder = MultipartBodyBuilder()

    bodyBuilder
      .part("index.html", ClassPathResource("/document/template/revocation-order/index.html").file.readText())
      .header("Content-Disposition", "form-data; name=index.html; filename=index.html")
    bodyBuilder
      .part("logo.png", ClassPathResource("/document/template/revocation-order/logo.png").file.readBytes())
      .header("Content-Disposition", "form-data; name=logo.png; filename=logo.png")

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
