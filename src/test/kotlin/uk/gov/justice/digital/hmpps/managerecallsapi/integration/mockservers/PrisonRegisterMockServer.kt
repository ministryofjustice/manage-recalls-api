package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

@Component
class PrisonRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(
  WireMockConfiguration().apply {
    port(9094)
    extensions(ResponseTemplateTransformer.builder().global(false).build())
  },
  "/health/ping"
) {

  val prisons =
    listOf(
      Prison(PrisonId("MWI"), PrisonName("Medway (STC)"), true),
      Prison(PrisonId("AKI"), PrisonName("Acklington (HMP)"), false),
      Prison(PrisonId("BMI"), PrisonName("Birmingham (HMP)"), true),
      Prison(PrisonId("KTI"), PrisonName("KTI (HMP)"), true),
      Prison(PrisonId("BAI"), PrisonName("BAI (HMP)"), true),
      Prison(PrisonId("BLI"), PrisonName("BLI (HMP)"), true),
      Prison(PrisonId("CFI"), PrisonName("Cardiff (HMP)"), true)
    )

  fun stubPrisons() {

    stubGet("/prisons", prisons, objectMapper)
  }
}
