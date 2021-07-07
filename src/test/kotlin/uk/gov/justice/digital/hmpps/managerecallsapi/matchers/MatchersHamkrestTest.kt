package uk.gov.justice.digital.hmpps.managerecallsapi.matchers

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.nothing
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MatchersHamkrestTest {

  @Nested
  inner class IsSingleItemMatching {
    @Test
    fun `should not match empty list`() {
      assertThat(emptyList(), !isSingleItemMatching(anything))
    }

    @Test
    fun `should not match single item if sub-matcher does not match`() {
      assertThat(listOf(Object()), !isSingleItemMatching(nothing))
    }

    @Test
    fun `should match single item if sub-matcher matches`() {
      assertThat(listOf(Object()), isSingleItemMatching(anything))
    }

    @Test
    fun `should not match more than one item`() {
      assertThat(listOf(Object(), Object()), !isSingleItemMatching(anything))
    }
  }
}
