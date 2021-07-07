package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException
import java.io.InputStream

@Component
class PdfDocumentGenerator(
  private val webClient: WebClient,
  @Value("\${gotenberg.endpoint.url}") private val gotenbergEndpointUrl: String,
  private val revocationOrder: RevocationOrder
) {

  fun makePdf(): ByteArray {
    val documentBody = MultipartBodyBuilder().apply {
      revocationOrder.details.forEach { documentDetail ->
        this
          .part(documentDetail.name, documentDetail.inputStream())
          .header(CONTENT_DISPOSITION, "form-data; name=${documentDetail.name}; filename=${documentDetail.name}")
      }
    }.build()

    return webClient
      .post()
      .uri("$gotenbergEndpointUrl/convert/html")
      .contentType(MULTIPART_FORM_DATA)
      .bodyValue(documentBody)
      .retrieve()
      .bodyToMono(ByteArray::class.java)
      .block()!!
  }
}

@Component
class RevocationOrder {
  val details = listOf(
    DocumentDetail("index.html", "/document/template/revocation-order/index.html"),
    DocumentDetail("logo.png", "/document/template/revocation-order/logo.png")
  )
}

data class DocumentDetail(val name: String, val path: String) {
  fun inputStream() = MultipartInputStreamFileResource(ClassPathResource(path).inputStream, name)
}

class MultipartInputStreamFileResource(inputStream: InputStream, private val filename: String) :
  InputStreamResource(inputStream) {
  override fun getFilename(): String {
    return filename
  }

  @Throws(IOException::class)
  override fun contentLength(): Long {
    return -1
  }
}
