package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.absent
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
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RevocationOrderGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()
  private val fixedClock = Clock.fixed(Instant.parse("2017-08-29T00:00:00.00Z"), ZoneId.of("UTC"))

  private val underTest = RevocationOrderGenerator(templateEngine, fixedClock)

  @Test
  fun `generate revocation order HTML with all values populated`() {
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
      ),
      Recall(::RecallId.random(), randomNoms(), lastReleaseDate = LocalDate.of(2020, 9, 1))
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
        has("licenseRevocationDate", { it.variable("licenseRevocationDate") }, equalTo("29 Aug 2017")),
        has("lastReleaseDate", { it.variable("lastReleaseDate") }, equalTo("01 Sept 2020")),
      )
    )
  }

  @Test
  fun `generate revocation order HTML with no values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("revocation-order", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      Prisoner(),
      Recall(::RecallId.random(), randomNoms())
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("firstNames", { it.variable("firstNames") }, equalTo("null null")),
        has("lastName", { it.variable("lastName") }, absent()),
        has("dateOfBirth", { it.variable("dateOfBirth") }, absent()),
        has("prisonNumber", { it.variable("prisonNumber") }, absent()),
        has("croNumber", { it.variable("croNumber") }, absent()),
        has("licenseRevocationDate", { it.variable("licenseRevocationDate") }, equalTo("29 Aug 2017")),
        has("lastReleaseDate", { it.variable("lastReleaseDate") }, absent()),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
