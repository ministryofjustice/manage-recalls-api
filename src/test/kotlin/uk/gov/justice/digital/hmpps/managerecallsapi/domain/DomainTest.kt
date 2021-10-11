package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class DomainTest {

  @Test
  fun `nomsNumber should be alphaNumeric and not blank`() {
    assertValid(::NomsNumber, "A1234AA", "1", "A")
    assertInvalid(::NomsNumber, "", " ", "-", "@")
  }

  @Test
  fun `FirstName should be not blank`() {
    assertValid(::FirstName, "Mr. Smith", "1", "A")
    assertInvalid(::FirstName, "", " ", "      ")
  }

  @Test
  fun `LastName should be not blank`() {
    assertValid(::LastName, "Mr. O'Badger", "1", "A")
    assertInvalid(::LastName, "", " ", "      ")
  }

  /**
   * The prison register is actually inconsistent here (https://github.com/ministryofjustice/prison-register)
   * The Api spec says it can be between 2 and 12 characters long
   * The database field has a maximum length of 6 characters
   * Choosing minimum length 2 and maximum length 6
   */
  @Test
  fun `PrisonId should be not blank and between 2 and 6 characters long`() {
    assertValid(::PrisonId, "XX", "12", "123456")
    assertInvalid(::PrisonId, " ", "  ", "X", "1234567")
  }

  private fun <V, T : Validated<V>> assertValid(fn: (V) -> T, vararg valid: V) = valid.map(fn)
  private fun <V, T : Validated<V>> assertInvalid(fn: (V) -> T, vararg invalid: V) = invalid.forEach {
    assertThat("$it", { fn(it) }, throws<IllegalArgumentException>())
  }
}
