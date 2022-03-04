package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
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

    val currentPrisonName = PrisonName("Prison (ABC)")
    val bookingNumber = "ABC123F"
    val tableOfContentsItems = listOf(TableOfContentsItem("Document 1", 1))

    val result = underTest.generateHtml(
      TableOfContentsContext(
        FullName("Bertie Badger"),
        RecallLengthDescription(recallLength),
        currentPrisonName,
        bookingNumber,
        2,
        FIXED
      ),
      tableOfContentsItems
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("logoFileName", { it.variableAsString("logoFileName") }, equalTo(HmppsLogo.fileName.value)),
        has(
          "recallLengthAndSentenceHeading",
          { it.variableAsString("recallLengthAndSentenceHeading") },
          equalTo(expectedText)
        ),
        has("fullName", { it.variableAsString("fullName") }, equalTo("Bertie Badger")),
        has("currentPrisonName", { it.variableAsString("currentPrisonName") }, equalTo(currentPrisonName.value)),
        has("category", { it.variableAsString("category") }, equalTo("Not Applicable")),
        has("bookingNumber", { it.variableAsString("bookingNumber") }, equalTo(bookingNumber)),
        has("version", { it.variableAsString("version") }, equalTo("0 (2)")),
        has("tableOfContentsItems", { it.variable("tableOfContentsItems") }, equalTo(tableOfContentsItems)),
      )
    )
  }

  private fun IContext.variableAsString(variableName: String) = getVariable(variableName)?.toString()
  private inline fun <reified T : Any> IContext.variable(variableName: String): T = getVariable(variableName) as T
}
