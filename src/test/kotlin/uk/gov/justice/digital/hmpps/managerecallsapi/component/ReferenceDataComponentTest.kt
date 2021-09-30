package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnitResponse

class ReferenceDataComponentTest : ComponentTestBase() {

  @Test
  fun `can get local delivery unit`() {
    val response = authenticatedClient.localDeliveryUnit()

    assertThat(response.size, equalTo(LocalDeliveryUnit.values().size))
    assertThat(response[0], equalTo(LocalDeliveryUnitResponse(LocalDeliveryUnit.values().get(0).name, LocalDeliveryUnit.values()[0].label)))
  }
}
