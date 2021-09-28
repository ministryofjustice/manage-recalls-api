package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.thymeleaf.context.IContext
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class ReasonsForRecallGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = ReasonsForRecallGenerator(templateEngine)

  @Test
  fun `generate recall summary HTML with all values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("reasons-for-recall", capture(contextSlot)) } returns expectedHtml

    val nomsNumber: NomsNumber = randomNoms()
    val result = underTest.generateHtml(
      ReasonsForRecallContext(
        Recall(
          ::RecallId.random(),
          nomsNumber,
          bookingNumber = "B1234",
          licenceConditionsBreached = "(i) breach one\n(ii) breach two"
        ),
        Prisoner(
          firstName = "Bertie",
          middleNames = "Basset",
          lastName = "Badger",
          bookNumber = "bookNumber"
        )
      )
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("firstNames", { it.variable("firstNames") }, equalTo("Bertie Basset")),
        has("lastName", { it.variable("lastName") }, equalTo("Badger")),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo("B1234")),
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo(nomsNumber.value)),
        has(
          "licenceConditionsBreached", { it.variable("licenceConditionsBreached") },
          equalTo("(i) breach one\n(ii) breach two")
        )
      )
    )
  }

  @Test
  fun `generate recall summary HTML with no values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("reasons-for-recall", capture(contextSlot)) } returns expectedHtml

    val nomsNumber = randomNoms()
    val result = underTest.generateHtml(
      ReasonsForRecallContext(Recall(::RecallId.random(), nomsNumber), Prisoner())
    )
    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("firstNames", { it.variable("firstNames") }, equalTo("")),
        has("lastName", { it.variable("lastName") }, equalTo(null)),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo(null)),
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo(nomsNumber.value)),
        has("licenceConditionsBreached", { it.variable("licenceConditionsBreached") }, equalTo(null))
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
