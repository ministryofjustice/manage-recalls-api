package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdfDocumentGeneratorIntegrationTest {

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  lateinit var pdfDocumentGenerator: PdfDocumentGenerator

  @BeforeAll
  fun startMocks() {
    gotenbergMockServer.start()
  }

  @AfterAll
  fun stopMocks() {
    gotenbergMockServer.stop()
  }

  @Test
  fun `should return byte array when requesting pdf`() {

    gotenbergMockServer
      .stubFor(
        post(urlEqualTo("/convert/html"))
          .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.MULTIPART_FORM_DATA_VALUE))
          .withMultipartRequestBody(
            aMultipart()
              .withName("files")
              .withHeader("Content-Disposition", WireMock.equalTo("form-data; name=index.html; filename=index.html"))
              .withBody(WireMock.equalTo(ClassPathResource("/document/template/revocation-order/index.html").file.readText()))
          )
          .withMultipartRequestBody(
            aMultipart()
              .withName("files")
              .withHeader("Content-Disposition", WireMock.equalTo("form-data; name=logo.png; filename=logo.png"))
              .withBody(WireMock.equalTo(ClassPathResource("/document/template/revocation-order/logo.png").file.readText()))
          )
          .willReturn(aResponse().withBody("test".toByteArray()))
      )

    val makePdfResult = pdfDocumentGenerator.makePdf()
    assertThat(String(makePdfResult), equalTo("test"))
  }
}
