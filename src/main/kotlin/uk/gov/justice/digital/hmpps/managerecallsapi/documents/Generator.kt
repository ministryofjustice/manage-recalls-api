package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

class Generator {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  @Qualifier("gotenbergWebClient")
  internal lateinit var webClient: WebClient

  fun makePdf(): WebClient.RequestHeadersSpec<*> {
    val bodyBuilder = MultipartBodyBuilder()

    bodyBuilder.part("files", ClassPathResource("/document/template/revocation-order/index.html").file.readBytes())
    bodyBuilder.part("files", ClassPathResource("/document/template/revocation-order/logo.png").file.readBytes())

    return webClient
      .post()
      .uri("/convert/html")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .bodyValue(BodyInserters.fromMultipartData(bodyBuilder.build()))

  }
}