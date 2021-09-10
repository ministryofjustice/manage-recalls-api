package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.InputStream

@Component
class PdfDocumentGenerator(
  private val webClient: WebClient,
  @Value("\${gotenberg.endpoint.url}") private val gotenbergEndpointUrl: String,
) {

  fun makePdf(details: List<DocumentDetail<out Any>>): Mono<ByteArray> {
    val documentBody = multipartBody(details)

    return gotenbergResponse("/convert/html", documentBody)
  }

  fun mergePdfs(details: List<DocumentDetail<out Any>>): Mono<ByteArray> {
    val documentBody = multipartBody(details)

    // TODO This exposes the gotenberg internal feature of how the inputs are ordered - alphabetical by filename - in the output - which should be hidden from callers
    return gotenbergResponse("/merge", documentBody)
  }

  private fun multipartBody(details: List<DocumentDetail<out Any>>) =
    MultipartBodyBuilder().apply {
      details.forEach { documentDetail ->
        this
          .part(documentDetail.name, documentDetail.data())
          .header(CONTENT_DISPOSITION, "form-data; name=${documentDetail.name}; filename=${documentDetail.name}")
      }
    }.build()

  private fun gotenbergResponse(
    path: String,
    documentBody: MultiValueMap<String, HttpEntity<*>>
  ) = webClient
    .post()
    .uri("$gotenbergEndpointUrl$path")
    .contentType(MULTIPART_FORM_DATA)
    .bodyValue(documentBody)
    .retrieve()
    .bodyToMono(ByteArray::class.java)
}

interface DocumentDetail<T> {
  val name: String
  fun data(): T
}

data class ClassPathDocumentDetail(override val name: String, val path: String) :
  DocumentDetail<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(ClassPathResource(path).inputStream, name)
}

data class InputStreamDocumentDetail(override val name: String, val inputStream: InputStream) :
  DocumentDetail<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(inputStream, name)
}

// TODO: rename `HtmlDocumentDetail` as simply `StringDocumentDetail`?
data class HtmlDocumentDetail(override val name: String, val html: String) : DocumentDetail<String> {
  override fun data() = html
}

// TODO: rename - remove 'file'
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
