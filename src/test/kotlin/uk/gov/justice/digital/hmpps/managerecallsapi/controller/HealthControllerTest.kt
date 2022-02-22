package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class HealthControllerTest {
  private val underTest = HealthController()

  @Test
  fun `getHealth returns OK`() {
    val result = underTest.getHealth()
    val expected = hashMapOf(
      "status" to "UP"
    )

    assertThat(result, equalTo(expected))
  }
}
