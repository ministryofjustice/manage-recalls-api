package uk.gov.justice.digital.hmpps.managerecallsapi.matchers

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
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

fun hasTotalPageCount(expectedNumberOfPages: Int): Matcher<Pdf> =
  has(
    "number of pages",
    { Base64.getDecoder().decode(it.content.toByteArray()) },
    isPdfWithWithTotalPageCount(expectedNumberOfPages)
  )

fun isPdfWithWithTotalPageCount(expectedNumberOfPages: Int): Matcher<ByteArray> =
  object : Matcher<ByteArray> {
    override val description: String = "contains the total page count of $expectedNumberOfPages"

    override fun invoke(actual: ByteArray): MatchResult =
      PdfReader(actual).use { pdfReader ->
        val recallSummaryText = PdfTextExtractor(pdfReader).getTextFromPage(1)
        return when {
          recallSummaryText.contains("of pages $expectedNumberOfPages (includes this one)") -> Match
          else -> Mismatch("page content '$recallSummaryText' does not contain expected page count $expectedNumberOfPages")
        }
      }
  }

fun <T> onlyContainsInOrder(expected: List<Matcher<T>>): Matcher<List<T>> =
  object : Matcher<List<T>> {
    override val description: String = "contains only the expected items in the same order ${expected.map { it.description }}"

    override fun invoke(actual: List<T>): MatchResult {
      if (actual.size != expected.size) return Mismatch("size is ${actual.size}, expected ${expected.size}")

      val expectedIterator = expected.iterator()
      for (actualValue in actual) {
        expectedIterator.next().let { matcher ->
          matcher.invoke(actualValue).let { matchResult ->
            if (matchResult is Mismatch) return matchResult
          }
        }
      }
      return Match
    }
  }
