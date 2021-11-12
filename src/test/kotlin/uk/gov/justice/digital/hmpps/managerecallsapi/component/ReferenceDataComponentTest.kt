package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.IndexOffence
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.IndexOffenceResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnitResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court
import uk.gov.justice.digital.hmpps.managerecallsapi.register.Prison

class ReferenceDataComponentTest : ComponentTestBase() {

  @Test
  fun `can get local delivery unit`() {
    val response = unauthenticatedGetResponse("/reference-data/local-delivery-units")
      .expectBody(object : ParameterizedTypeReference<List<LocalDeliveryUnitResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = LocalDeliveryUnit.values().map { LocalDeliveryUnitResponse(it.name, it.label) }
    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get courts`() {
    val response = unauthenticatedGetResponse("/reference-data/courts")
      .expectBody(object : ParameterizedTypeReference<List<Court>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = listOf(
      Court(CourtId("ACCRYC"), CourtName("Accrington Youth Court")),
      Court(CourtId("BANBCT"), CourtName("Banbury County Court")),
      Court(CourtId("CARLCT"), CourtName("Carlisle Combined Court Centre")),
      Court(CourtId("HVRFCT"), CourtName("Haverfordwest County Court")),
    )

    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get prisons`() {
    val response = unauthenticatedGetResponse("/reference-data/prisons")
      .expectBody(object : ParameterizedTypeReference<List<Prison>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = listOf(
      Prison(PrisonId("MWI"), PrisonName("Medway (STC)"), true),
      Prison(PrisonId("AKI"), PrisonName("Acklington (HMP)"), false),
      Prison(PrisonId("BMI"), PrisonName("Birmingham (HMP)"), true),
      Prison(PrisonId("KTI"), PrisonName("KTI (HMP)"), true),
      Prison(PrisonId("BAI"), PrisonName("BAI (HMP)"), true),
      Prison(PrisonId("BLI"), PrisonName("BLI (HMP)"), true),
    )

    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get index offences`() {
    val response = unauthenticatedGetResponse("/reference-data/index-offences")
      .expectBody(object : ParameterizedTypeReference<List<IndexOffenceResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = IndexOffence.values().map { IndexOffenceResponse(it.name, it.group.name, it.label) }

    assertThat(response, equalTo(expectedResponse))
    assertThat(317, equalTo(expectedResponse.size))

    assertThat(
      IndexOffenceResponse(
        "ABDUCTION",
        "SEXUAL_OFFENCE_SERIOUS_GROUP",
        "Abduction"
      ),
      equalTo(expectedResponse[0])
    )

    assertThat(
      IndexOffenceResponse(
        "WOUNDING_WITH_INTENT_TO_CAUSE_GRIEVOUS_BODILY_HARM",
        "NOT_SPECIFIED_GROUP",
        "Wounding with intent to cause grievous bodily harm"
      ),
      equalTo(expectedResponse[316])
    )
  }
}
