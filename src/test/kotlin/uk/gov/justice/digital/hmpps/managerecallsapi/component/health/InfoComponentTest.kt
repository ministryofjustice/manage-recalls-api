package uk.gov.justice.digital.hmpps.managerecallsapi.component.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class InfoComponentTest : ComponentTestBase() {

  @Test
  fun `Info page is accessible and has build version`() {
    unauthenticatedGet("/info")
      .expectBody()
      .jsonPath("app.name").isEqualTo("Manage Recalls Api")
      .jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDate.now().format(ISO_DATE))
      }
  }
}
