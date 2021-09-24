package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

@Component
class PdfDocumentGenerationService(@Autowired private val gotenbergApi: GotenbergApi) {

  fun generatePdf(html: String, vararg images: ImageData<out Any>): Mono<ByteArray> =
    gotenbergApi.convertHtml(
      toConvertHtmlRequest(
        html,
        images,
        mapOf(
          "marginTop" to 0.0,
          "marginBottom" to 0.0,
          "marginLeft" to 0.0,
          "marginRight" to 0.0
        )
      )
    )

  fun mergePdfs(details: List<Data<MultipartInputStreamFileResource>>): Mono<ByteArray> =
    gotenbergApi.merge(details.toMergeRequest())

  private fun toConvertHtmlRequest(
    html: String,
    images: Array<out ImageData<out Any>>,
    additionalProperties: Map<String, Any> = emptyMap()
  ) =
    MultipartBodyBuilder().apply {
      addMultipart("index.html", html)
      images.forEach { image ->
        addMultipart(image.fileName, image.data())
      }
      additionalProperties.forEach {
        this.part(it.key, it.value)
      }
    }.build()

  private fun MultipartBodyBuilder.addMultipart(
    name: String,
    html: Any
  ) = part(name, html).header(CONTENT_DISPOSITION, "form-data; name=$name; filename=$name")

  private fun List<Data<MultipartInputStreamFileResource>>.toMergeRequest(additionalProperties: Map<String, Any> = emptyMap()) =
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

interface Data<T> {
  fun data(): T
}

interface ImageData<T> : Data<T> {
  val fileName: String
}

data class ClassPathImageData(val recallImage: RecallImage, override val fileName: String = recallImage.fileName) :
  ImageData<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(ClassPathResource(recallImage.path).inputStream, fileName)
}

data class Base64EncodedImageData(override val fileName: String, val base64EncodedContent: String) :
  ImageData<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(ByteArrayInputStream(Base64.getDecoder().decode(base64EncodedContent.toByteArray())), fileName)
}

open class ClassPathDocumentData(private val path: String) :
  Data<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(ClassPathResource(path).inputStream)
}

data class InputStreamDocumentData(val inputStream: InputStream) : Data<MultipartInputStreamFileResource> {
  override fun data() = MultipartInputStreamFileResource(inputStream)
}

class MultipartInputStreamFileResource(inputStream: InputStream, private val filename: String? = null) :
  InputStreamResource(inputStream) {
  override fun getFilename(): String? {
    return filename
  }

  @Throws(IOException::class)
  override fun contentLength(): Long {
    return -1
  }
}
