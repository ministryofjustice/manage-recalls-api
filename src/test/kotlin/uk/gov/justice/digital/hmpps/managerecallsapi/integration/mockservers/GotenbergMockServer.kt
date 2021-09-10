package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.stereotype.Component

@Component
class GotenbergMockServer : WireMockServer(9093) {
  fun stubPdfGeneration(generatedPdf: ByteArray, textOfHtml: String) {
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
            .withHeader("Content-Disposition", equalTo("form-data; name=logo.png; filename=logo.png"))
            .withBody(equalTo(ClassPathResource("/document/template/revocation-order/logo.png").file.readText()))
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
