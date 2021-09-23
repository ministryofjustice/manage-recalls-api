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

@Component
class GotenbergMockServer : WireMockServer(9093) {
  fun stubGenerateRevocationOrder(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubHtmlConversion(expectedTextInHtml, "revocation-order-logo.png", generatedPdfContents)
  }

  fun stubGetRecallNotification(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubHtmlConversion(expectedTextInHtml, "recall-summary-logo.png", generatedPdfContents)
  }

  fun stubLetterToProbation(generatedPdfContents: ByteArray, expectedTextInHtml: String) {
    stubHtmlConversion(expectedTextInHtml, "letter-to-probation-logo.png", generatedPdfContents)
  }

  private fun stubHtmlConversion(
      expectedTextInHtml: String,
      logoFileName: String,
      generatedPdfContents: ByteArray
  ) {
    stubFor(
      post(WireMock.urlEqualTo("/convert/html")).apply {
        withMultipartHeader()
        withMultipartFor("index.html", containing(expectedTextInHtml))
        withMultipartFor(logoFileName, equalTo(ClassPathResource("/templates/images/$logoFileName").file.readText()))
      }
        .willReturn(aResponse().withBody(generatedPdfContents))
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
    .withHeader("Content-Disposition", equalTo("form-data; name=${documentName}; filename=${documentName}"))
    .withBody(contentPattern)

  fun stubPdfGeneration(generatedPdf: ByteArray, textOfHtml: String, logoFileName: String) {
    stubFor(
      post(WireMock.urlEqualTo("/convert/html"))
        .withHeader(CONTENT_TYPE, containing(MULTIPART_FORM_DATA_VALUE))
        .withMultipartRequestBody(
          aMultipart()
            .withName("files")
            .withHeader("Content-Disposition", equalTo("form-data; name=index.html; filename=index.html"))
            .withBody(containing(textOfHtml))
        )
        .withMultipartRequestBody(
          aMultipart()
            .withName("files")
            .withHeader("Content-Disposition", equalTo("form-data; name=$logoFileName.png; filename=$logoFileName.png"))
            .withBody(equalTo(ClassPathResource("/templates/images/$logoFileName.png").file.readText()))
        )
        .willReturn(aResponse().withBody(generatedPdf))
    )
  }

  fun stubMergePdfs(
    generatedPdf: ByteArray,
    vararg fileDetails: Pair<String, String>
  ) {
    stubFor(
      post(WireMock.urlEqualTo("/merge"))
        .withHeader(CONTENT_TYPE, containing(MULTIPART_FORM_DATA_VALUE))
        .apply {
          fileDetails.forEach { fileDetails ->
            this.withMultipartRequestBody(
              aMultipart()
                .withHeader(
                  "Content-Disposition",
                  equalTo("form-data; name=${fileDetails.first}; filename=${fileDetails.first}")
                )
                .withBody(equalTo(fileDetails.second))
            )
          }
        }.willReturn(aResponse().withBody(generatedPdf))

    )
  }

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
