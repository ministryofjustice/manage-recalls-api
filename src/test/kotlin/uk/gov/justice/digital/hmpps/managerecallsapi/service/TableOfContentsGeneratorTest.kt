package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.thymeleaf.context.IContext
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableOfContentsGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = TableOfContentsGenerator(templateEngine)

  private fun sentencingInfoOptions(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(SentenceLength(0, 11, 0), "14 Day FTR under 12 months"),
      Arguments.of(SentenceLength(1, 0, 0), "28 Day FTR 12 months & over")
    )
  }

  @ParameterizedTest(name = "generate TOC HTML with all values populated for {0}")
  @MethodSource("sentencingInfoOptions")
  fun `generate TOC HTML with all values populated`(sentencingLength: SentenceLength, expectedText: String) {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("table-of-contents", capture(contextSlot)) } returns expectedHtml

    val currentPrisonName = "Prison (ABC)"
    val bookingNumber = "ABC123F"
    val result = underTest.generateHtml(
      TableOfContentsContext(
        Recall(
          ::RecallId.random(), randomNoms(),
          lastReleaseDate = LocalDate.of(2020, 9, 1),
          bookingNumber = bookingNumber,
          sentencingInfo = SentencingInfo(now(), now(), now(), "Court", "", sentencingLength)
        ),
        Prisoner(
          firstName = "Bertie",
          middleNames = "Basset",
          lastName = "Badger",
          dateOfBirth = LocalDate.of(1995, 10, 3),
          bookNumber = "bookNumber",
          croNumber = "croNumber"
        ),
        currentPrisonName,
        listOf(Document("Document 1", 1))
      )
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has(
          "recallLengthAndSentenceHeading", { it.variable("recallLengthAndSentenceHeading") }, equalTo(expectedText)
        ),
        has("fullName", { it.variable("fullName") }, equalTo("Bertie Basset Badger")),
        has("establishment", { it.variable("establishment") }, equalTo(currentPrisonName)),
        has("category", { it.variable("category") }, equalTo("Not Applicable")),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo(bookingNumber)),
        has("version", { it.variable("version") }, equalTo("0")),
        has("documents", { it.variable("documents") }, equalTo("[Document(title=Document 1, pageNumber=1)]")),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}