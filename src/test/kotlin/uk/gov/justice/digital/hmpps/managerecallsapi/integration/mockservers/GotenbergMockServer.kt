package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.matching.ContentPattern
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo

@Component
class GotenbergMockServer : WireMockServer(9093) {
  fun stubGenerateRevocationOrder(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubPdfGeneration(generatedPdfContents, expectedTextInHtml, RevocationOrderLogo)
  }

  fun stubGenerateRecallSummary(generatedPdfContents: ByteArray) {
    stubPdfGenerationWithHmppsLogo(generatedPdfContents, "OFFENDER IS IN CUSTODY")
  }

  fun stubGenerateLetterToProbation(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubPdfGenerationWithHmppsLogo(generatedPdfContents, expectedTextInHtml)
  }

  fun stubGenerateTableOfContents(generatedPdfContents: ByteArray) {
    stubPdfGenerationWithHmppsLogo(generatedPdfContents, "PAPERS FOR THE PAROLE BOARD RELATING TO")
  }

  fun stubGenerateReasonsForRecall(generatedPdfContents: ByteArray) {
    stubPdfGeneration(generatedPdfContents, "LICENCE REVOCATION")
  }

  private fun stubPdfGenerationWithHmppsLogo(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubPdfGeneration(generatedPdfContents, expectedTextInHtml, HmppsLogo)
  }

  private fun stubPdfGeneration(
    generatedPdfContents: ByteArray,
    expectedTextInHtml: String,
    vararg recallImage: RecallImage
  ) {
    stubFor(
      post(WireMock.urlEqualTo("/convert/html")).apply {
        withMultipartHeader()
        withMultipartFor("index.html", containing(expectedTextInHtml))
        recallImage.forEach { image ->
          withMultipartFor(image.fileName, equalTo(ClassPathResource(image.path).file.readText()))
        }
      }
        .willReturn(aResponse().withBody(generatedPdfContents))
    )
  }

  fun stubMergePdfs(
    generatedPdf: ByteArray,
    vararg fileContentsToMerge: String
  ) {
    stubFor(
      post(WireMock.urlEqualTo("/merge"))
        .apply {
          withMultipartHeader()
          fileContentsToMerge.forEachIndexed { index, fileContents ->
            withMultipartFor("$index.pdf", equalTo(fileContents))
          }
        }.willReturn(aResponse().withBody(generatedPdf))
    )
  }

  private fun MappingBuilder.withMultipartHeader() {
    this.withHeader(CONTENT_TYPE, containing(MULTIPART_FORM_DATA_VALUE))
  }

  private fun <T> MappingBuilder.withMultipartFor(fileName: String, contentPattern: ContentPattern<T>) {
    this.withMultipartRequestBody(multipartFor(fileName, contentPattern))
  }

  private fun <T> multipartFor(documentName: String, contentPattern: ContentPattern<T>) = aMultipart()
    .withName("files")
    .withHeader("Content-Disposition", equalTo("form-data; name=$documentName; filename=$documentName"))
    .withBody(contentPattern)

  fun isHealthy() {
    healthCheck(OK)
  }

  fun isUnhealthy() {
    healthCheck(INTERNAL_SERVER_ERROR)
  }

  private fun healthCheck(status: HttpStatus) =
    this.stubFor(
      WireMock.get("/ping").willReturn(
        aResponse().withStatus(status.value())
      )
    )
}
