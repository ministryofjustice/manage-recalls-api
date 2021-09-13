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
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RevocationOrderGeneratorTest() {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = RevocationOrderGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("revocation-order", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      Prisoner(
        firstName = "Bertie",
        middleNames = "Basset",
        lastName = "Badger",
        dateOfBirth = LocalDate.of(1995, 10, 3),
        bookNumber = "bookNumber",
        croNumber = "croNumber"
      )
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("firstNames", { it.variable("firstNames") }, equalTo("Bertie Basset")),
        has("lastName", { it.variable("lastName") }, equalTo("Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("1995-10-03")),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo("bookNumber")),
        has("croNumber", { it.variable("croNumber") }, equalTo("croNumber")),
        has(
          "licenseRevocationDate", { it.variable("licenseRevocationDate") }, equalTo((LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))),
        )
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName).toString()
}
