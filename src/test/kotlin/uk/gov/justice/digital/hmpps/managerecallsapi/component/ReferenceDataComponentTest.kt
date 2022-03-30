package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.PoliceForce
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnitResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevelResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallReasonResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReasonResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court
import java.time.LocalDate

class ReferenceDataComponentTest : ComponentTestBase() {

  @Test
  fun `can get local delivery unit`() {
    val response = unauthenticatedGetResponse("/reference-data/local-delivery-units")
      .expectBody(object : ParameterizedTypeReference<List<LocalDeliveryUnitResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = LocalDeliveryUnit.values().map { LocalDeliveryUnitResponse(it.name, it.label, it.isActiveOn(LocalDate.now())) }
    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get recall reasons`() {
    val response = unauthenticatedGetResponse("/reference-data/recall-reasons")
      .expectBody(object : ParameterizedTypeReference<List<RecallReasonResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = ReasonForRecall.values().map { RecallReasonResponse(it.name, it.label) }
    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get stop reasons`() {
    val response = unauthenticatedGetResponse("/reference-data/stop-reasons")
      .expectBody(object : ParameterizedTypeReference<List<StopReasonResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = StopReason.values().map { StopReasonResponse(it.name, it.label, it.validForStopCall) }
    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get mappa levels`() {
    val response = unauthenticatedGetResponse("/reference-data/mappa-levels")
      .expectBody(object : ParameterizedTypeReference<List<MappaLevelResponse>>() {})
      .returnResult()
      .responseBody!!

    val expectedResponse = MappaLevel.values().map { MappaLevelResponse(it.name, it.label) }
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
      Court(CourtId("SOUTCT"), CourtName("Southport County Court")),
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
      Prison(PrisonId("CFI"), PrisonName("Cardiff (HMP)"), true),
    )

    assertThat(response, equalTo(expectedResponse))
  }

  @Test
  fun `can get police forces`() {
    val response = unauthenticatedGetResponse("/reference-data/police-forces")
      .expectBody(object : ParameterizedTypeReference<List<PoliceForce>>() {})
      .returnResult()
      .responseBody!!

    assertThat(response.size, equalTo(44))

    assertThat(
      response[0],
      equalTo(
        PoliceForce(
          PoliceForceId("avon-and-somerset"),
          PoliceForceName("Avon and Somerset Constabulary")
        )
      )
    )

    assertThat(
      response[43],
      equalTo(
        PoliceForce(
          PoliceForceId("wiltshire"),
          PoliceForceName("Wiltshire Police")
        )
      )
    )
  }
}
