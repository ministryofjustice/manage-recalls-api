package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court

@Component
class CourtRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(
  WireMockConfiguration().apply {
    port(9095)
    extensions(ResponseTemplateTransformer.builder().global(false).build())
  },
  "/health/ping"
) {

  val courts =
    listOf(
      Court(CourtId("ACCRYC"), CourtName("Accrington Youth Court")),
      Court(CourtId("BANBCT"), CourtName("Banbury County Court")),
      Court(CourtId("CARLCT"), CourtName("Carlisle Combined Court Centre")),
      Court(CourtId("HVRFCT"), CourtName("Haverfordwest County Court")),
      Court(CourtId("SOUTCT"), CourtName("Southport County Court")),
    )

  fun stubCourts() {
    stubGet("/courts/all", courts, objectMapper)
  }
}
