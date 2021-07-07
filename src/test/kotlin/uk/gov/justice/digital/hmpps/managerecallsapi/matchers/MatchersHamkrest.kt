package uk.gov.justice.digital.hmpps.managerecallsapi.matchers

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.cast
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize

fun <T> isSingleItemMatching(matcher: Matcher<T>): Matcher<Collection<T>> =
  allOf(
    cast(hasSize(equalTo(1))),
    object : Matcher<Collection<T>> {
      override val description = matcher.description

      override fun invoke(actual: Collection<T>): MatchResult {
        return matcher(actual.iterator().next())
      }
    }
  )
