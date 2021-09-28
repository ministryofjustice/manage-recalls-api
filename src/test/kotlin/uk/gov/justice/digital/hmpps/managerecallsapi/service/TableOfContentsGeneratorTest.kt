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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableOfContentsGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = TableOfContentsGenerator(templateEngine)

  private fun recallLengthOptions(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(FOURTEEN_DAYS, "14 Day FTR under 12 months"),
      Arguments.of(TWENTY_EIGHT_DAYS, "28 Day FTR 12 months & over")
    )
  }

  @ParameterizedTest(name = "generate TOC HTML with all values populated for {0}")
  @MethodSource("recallLengthOptions")
  fun `generate TOC HTML with all values populated`(recallLength: RecallLength, expectedText: String) {
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
          recallLength = recallLength
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
        has("logoFileName", { it.variable("logoFileName") }, equalTo(HmppsLogo.fileName)),
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
  