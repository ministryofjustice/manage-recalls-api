package uk.gov.justice.digital.hmpps.managerecallsapi.component.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = ["management.server.port=9998", "server.port=9998"])
class InfoComponentTest : ComponentTestBase() {

  @Test
  fun `Info page is accessible and has build version`() {
    unauthenticatedGet("/info")
      .jsonPath("app.name").isEqualTo("Manage Recalls Api")
      .jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDate.now().format(ISO_DATE))
      }
  }
}
