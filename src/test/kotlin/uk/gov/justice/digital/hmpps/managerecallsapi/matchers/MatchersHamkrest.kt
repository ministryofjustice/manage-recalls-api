package uk.gov.justice.digital.hmpps.managerecallsapi.matchers

import com.lowagie.text.pdf.PdfReader
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.cast
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import java.util.Base64

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

fun hasNumberOfPages(numberOfPagesMatcher: Matcher<Int>): Matcher<Pdf> =
  has(
    "number of pages",
    { Base64.getDecoder().decode(it.content.toByteArray()) },
    isPdfWithNumberOfPages(numberOfPagesMatcher)
  )

fun isPdfWithNumberOfPages(numberOfPagesMatcher: Matcher<Int>): Matcher<ByteArray> =
  object : Matcher<ByteArray> {
    override val description: String = numberOfPagesMatcher.description

    override fun invoke(actual: ByteArray): MatchResult =
      PdfReader(actual).use { pdfReader ->
        numberOfPagesMatcher(pdfReader.numberOfPages)
      }
  }
