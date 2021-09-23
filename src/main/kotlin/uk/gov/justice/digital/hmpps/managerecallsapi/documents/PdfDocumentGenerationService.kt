package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.InputStream

@Component
class PdfDocumentGenerationService(@Autowired private val gotenbergApi: GotenbergApi) {

  fun generatePdf(html: String, vararg assets: DocumentDetail<out Any>): Mono<ByteArray> =
    gotenbergApi.convertHtml(
      toConvertHtmlRequest(
        html,
        assets,
        mapOf(
          "marginTop" to 0.0,
          "marginBottom" to 0.0,
          "marginLeft" to 0.0,
          "marginRight" to 0.0
        )
      )
    )

  fun mergePdfs(details: List<DocumentDetail<out Any>>): Mono<ByteArray> =
    gotenbergApi.merge(details.toMergeRequest())

  private fun toConvertHtmlRequest(html: String, assets: Array<out DocumentDetail<out Any>>, additionalProperties: Map<String, Any> = emptyMap()) =
    MultipartBodyBuilder().apply {
      addMultipart("index.html", html)
      assets.forEach { documentDetail ->
        addMultipart(documentDetail.name, documentDetail.data())
      }
      additionalProperties.forEach {
        this.part(it.key, it.value)
      }
    }.build()

  private fun MultipartBodyBuilder.addMultipart(
      name: String,
      html: Any
  ) = part(name, html).header(CONTENT_DISPOSITION, "form-data; name=$name; filename=$name")

  private fun List<DocumentDetail<out Any>>.toMergeRequest(additionalProperties: Map<String, Any> = emptyMap()) =
    MultipartBodyBuilder().apply {
      forEachIndexed { index, documentDetail ->
        val documentName = "$index.pdf"
        part(documentName, documentDetail.data())
          .header(CONTENT_DISPOSITION, "form-data; name=$documentName; filename=$documentName")
      }
      additionalProperties.forEach {
        this.part(it.key, it.value)
      }
    }.build()
}

interface DocumentDetail<T> {
  val name: String
  fun data(): T
}

data class ClassPathDocumentDetail(override val name: String, val path: String = "/templates/images/$name") :
  DocumentDetail<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(ClassPathResource(path).inputStream, name)
}

data class InputStreamDocumentDetail(override val name: String, val inputStream: InputStream) :
  DocumentDetail<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(inputStream, name)
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
