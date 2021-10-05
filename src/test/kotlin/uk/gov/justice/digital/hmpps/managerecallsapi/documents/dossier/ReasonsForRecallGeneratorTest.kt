package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.FirstAndMiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReasonsForRecallContext

class ReasonsForRecallGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = ReasonsForRecallGenerator(templateEngine)

  @Test
  fun `generate recall summary HTML with all values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("reasons-for-recall", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      ReasonsForRecallContext(
        FirstAndMiddleNames(FirstName("Bertie"), MiddleNames("Basset")),
        LastName("Badger"),
        "B1234",
        NomsNumber("A1234AA"),
        "(i) breach one\n(ii) breach two"
      )
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("firstAndMiddleNames", { it.variable("firstAndMiddleNames") }, equalTo("Bertie Basset")),
        has("lastName", { it.variable("lastName") }, equalTo("Badger")),
        has("bookingNumber", { it.variable("bookingNumber") }, equalTo("B1234")),
        has("nomsNumber", { it.variable("nomsNumber") }, equalTo("A1234AA")),
        has(
          "licenceConditionsBreached", { it.variable("licenceConditionsBreached") },
          equalTo("(i) breach one\n(ii) breach two")
        )
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
