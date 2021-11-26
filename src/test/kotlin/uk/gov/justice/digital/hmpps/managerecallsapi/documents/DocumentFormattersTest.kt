package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName

class DocumentFormattersTest {
  @Test
  fun `should format PersonName with middleNames`() {
    val personName = PersonName("Bernard", "McTavish", "Smythe")
    assertThat(personName.firstMiddleLast().value, equalTo("Bernard McTavish Smythe"))
    assertThat(personName.firstAndLastName().value, equalTo("Bernard Smythe"))
    assertThat(personName.toString(), equalTo("Bernard Smythe"))
  }

  @Test
  fun `should format PersonName without middleNames`() {
    val personName = PersonName(FirstName("Bernard"), null, LastName("Smythe"))
    assertThat(personName.toString(), equalTo("Bernard Smythe"))
    assertThat(personName.firstAndLastName().value, equalTo("Bernard Smythe"))
    assertThat(personName.firstMiddleLast().value, equalTo("Bernard Smythe"))
  }
}
