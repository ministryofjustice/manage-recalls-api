package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

@Component
class PdfDocumentGenerationService(@Autowired private val gotenbergApi: GotenbergApi) {

  fun generatePdf(html: String, vararg images: ImageData): Mono<ByteArray> =
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

  fun mergePdfs(details: List<Data>): Mono<ByteArray> =
    gotenbergApi.merge(details.toMergeRequest())

  private fun toConvertHtmlRequest(
    html: String,
    images: Array<out ImageData>,
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

  private fun List<Data>.toMergeRequest(additionalProperties: Map<String, Any> = emptyMap()) =
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

interface Data {
  fun data(): InputStreamResource

  companion object {
    fun documentData(resource: RecallClassPathResource): ByteArrayDocumentData = documentData(resource.byteArray())
    fun documentData(byteArray: ByteArray): ByteArrayDocumentData = ByteArrayDocumentData(byteArray)
  }
}

interface ImageData : Data {
  val fileName: String

  companion object {
    fun recallImage(recallImage: RecallImage): ImageData =
      ClassPathImageData(recallImage)

    fun signature(base64EncodedSignature: String): ImageData =
      Base64EncodedImageData("signature.jpg", base64EncodedSignature)
  }
}

private data class ClassPathImageData(
  val recallImage: RecallImage,
  override val fileName: String = recallImage.fileName
) : ImageData {
  override fun data() = MultipartInputStreamFileResource(ClassPathResource(recallImage.path).inputStream, fileName)
}

private data class Base64EncodedImageData(override val fileName: String, val base64EncodedContent: String) :
  ImageData {
  override fun data() = MultipartInputStreamFileResource(
    ByteArrayInputStream(
      Base64.getDecoder().decode(base64EncodedContent.toByteArray())
    ),
    fileName
  )
}

data class ByteArrayDocumentData(val byteArray: ByteArray, private val filename: String? = null) : Data {
  override fun data(): InputStreamResource = MultipartInputStreamFileResource(byteArray.inputStream())
}

internal class MultipartInputStreamFileResource(inputStream: InputStream, private val filename: String? = null) :
  InputStreamResource(inputStream) {
  override fun getFilename(): String? {
    return filename
  }

  @Throws(IOException::class)
  override fun contentLength(): Long {
    return -1
  }
}
