package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate

class RevocationOrderGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = RevocationOrderGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML with all values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("revocation-order", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      RevocationOrderContext(
        ::RecallId.random(),
        FullName("Bertie Badger"),
        LocalDate.of(1995, 10, 3),
        BookingNumber("bookNumber"),
        CroNumber("croNumber"),
        LocalDate.of(2017, 8, 29),
        LocalDate.of(2020, 9, 1),
        "userDetailsSignature",
        ::UserId.random(),
        "Badger Bertie"
      )
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("logoFileName", { it.variable("logoFileName") }, equalTo(RevocationOrderLogo.fileName.value)),
        has("prisonerNameOnLicence", { it.variable("prisonerNameOnLicence") }, equalTo("Bertie Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("03 Oct 1995")),
        has("bookingNumber", { it.variable("bookingNumber") }, equalTo("bookNumber")),
        has("croNumber", { it.variable("croNumber") }, equalTo("croNumber")),
        has("licenseRevocationDate", { it.variable("licenseRevocationDate") }, equalTo("29 Aug 2017")),
        has("lastReleaseDate", { it.variable("lastReleaseDate") }, equalTo("01 Sep 2020")),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
