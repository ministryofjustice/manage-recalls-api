package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class HealthControllerTest {
  private val underTest = HealthController()

  @Test
  fun `getHealth returns OK`() {
    val results = underTest.getHealth()

    assertThat(results.statusCodeValue, equalTo(200))
    assertThat(results.body, equalTo("OK"))
  }
}
