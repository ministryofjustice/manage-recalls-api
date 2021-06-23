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
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.stereotype.Component

@Component
class GotenbergMockServer : WireMockServer(9998) {
  fun stubPdfGeneration(generatedPdf: ByteArray) {
    stubFor(
      post(WireMock.urlEqualTo("/convert/html"))
        .withHeader(CONTENT_TYPE, containing(MULTIPART_FORM_DATA_VALUE))
        .withMultipartRequestBody(
          aMultipart()
            .withName("files")
            .withHeader("Content-Disposition", equalTo("form-data; name=index.html; filename=index.html"))
            .withBody(equalTo(ClassPathResource("/document/template/revocation-order/index.html").file.readText()))
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
}
