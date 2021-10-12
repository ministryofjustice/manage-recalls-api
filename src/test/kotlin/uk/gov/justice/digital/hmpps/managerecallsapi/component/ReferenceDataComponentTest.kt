package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnitResponse

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
}
