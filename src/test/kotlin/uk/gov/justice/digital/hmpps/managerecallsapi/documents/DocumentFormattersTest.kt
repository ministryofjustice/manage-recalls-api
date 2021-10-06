package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName

class DocumentFormattersTest {
  @Test
  fun `should format PersonName with middleNames`() {
    assertThat(
      PersonName("Bernard", "McTavish", "Smythe").toString(),
      equalTo("Bernard McTavish Smythe")
    )
  }

  @Test
  fun `should format PersonName without middleNames`() {
    assertThat(PersonName(FirstName("Bernard"), null, LastName("Smythe")).toString(), equalTo("Bernard Smythe"))
  }
}
