package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.info.BuildProperties
import java.util.Properties

class HealthInfoTest {
  @Test
  fun `should include version info`() {
    val properties = Properties()
    properties.setProperty("version", "somever")
    Assertions.assertThat(HealthInfo(BuildProperties(properties)).health().details)
      .isEqualTo(
        mapOf(
          "version" to "somever",
          "build_url" to "BUILD_URL not defined"
        )
      )
  }
}
