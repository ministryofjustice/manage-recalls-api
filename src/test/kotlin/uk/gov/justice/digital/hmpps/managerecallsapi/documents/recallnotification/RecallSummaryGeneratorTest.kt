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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_BARKING_DAGENHAM_HAVERING
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_TOWER_HAMLETS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecallSummaryGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()

  private val underTest = RecallSummaryGenerator(templateEngine)

  @Test
  fun `generate recall summary HTML with all values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("recall-summary", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      RecallSummaryContext(
        ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.of(19, 32), ZoneId.of("Europe/London")),
        PersonName("Bertie", "Basset", "Badger"),
        LocalDate.of(1995, 10, 3),
        "croNumber",
        PersonName("Maria", lastName = "Badger"),
        Email("maria@thebadgers.set"),
        PhoneNumber("09876543210"),
        LEVEL_3,
        SentenceLength(2, 3, 10),
        "Some offence",
        "High Court",
        LocalDate.of(2020, 10, 1),
        LocalDate.of(2020, 10, 29),
        "Mr Probation Officer",
        "01234567890",
        PS_BARKING_DAGENHAM_HAVERING,
        "Bryan Badger",
        "B1234",
        NomsNumber("AA1234A"),
        LocalDate.of(2020, 10, 1),
        setOf(POOR_BEHAVIOUR_FURTHER_OFFENCE),
        "London",
        true,
        "Some contraband detail",
        true,
        "Some stuff",
        PrisonName("Current Prison"),
        PrisonName("Last Release Prison")
      ),
      3
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("logoFileName", { it.variable("logoFileName") }, equalTo(HmppsLogo.fileName)),
        has("createdDate", { it.variable("createdDate") }, equalTo("01 Sep 2021")),
        has("createdTime", { it.variable("createdTime") }, equalTo("19:32")),
        has("recallNotificationTotalNumberOfPages", { it.variable("recallNotificationTotalNumberOfPages") }, equalTo("3")),
        has("assessedByUserName", { it.variable("assessedByUserName") }, equalTo("Maria Badger")),
        has("assessedByUserEmail", { it.variable("assessedByUserEmail") }, equalTo("maria@thebadgers.set")),
        has("assessedByUserPhoneNumber", { it.variable("assessedByUserPhoneNumber") }, equalTo("09876543210")),
        has("mappaLevel1", { it.variable("mappaLevel1") }, equalTo("false")),
        has("mappaLevel2", { it.variable("mappaLevel2") }, equalTo("false")),
        has("mappaLevel3", { it.variable("mappaLevel3") }, equalTo("true")),
        has("firstAndMiddleNames", { it.variable("firstAndMiddleNames") }, equalTo("Bertie Basset")),
        has("lastName", { it.variable("lastName") }, equalTo("Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("03 Oct 1995")),
        has("previousConvictionMainName", { it.variable("previousConvictionMainName") }, equalTo("Bryan Badger")),
        has("bookingNumber", { it.variable("bookingNumber") }, equalTo("B1234")),
        has("nomsNumber", { it.variable("nomsNumber") }, equalTo("AA1234A")),
        has("lastReleasePrisonName", { it.variable("lastReleasePrisonName") }, equalTo("Last Release Prison")),
        has("localDeliveryUnit", { it.variable("localDeliveryUnit") }, equalTo("PS - Barking, Dagenham & Havering")),
        has("lastReleaseDate", { it.variable("lastReleaseDate") }, equalTo("01 Oct 2020")),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo("2 years 3 months 10 days")),
        has("indexOffence", { it.variable("indexOffence") }, equalTo("Some offence")),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("true")),
        has("croNumber", { it.variable("croNumber") }, equalTo("croNumber")),
        has("probationOfficerName", { it.variable("probationOfficerName") }, equalTo("Mr Probation Officer")),
        has("probationOfficerPhoneNumber", { it.variable("probationOfficerPhoneNumber") }, equalTo("01234567890")),
        has("localPoliceForce", { it.variable("localPoliceForce") }, equalTo("London")),
        has("currentPrisonName", { it.variable("currentPrisonName") }, equalTo("Current Prison")),
        has("sentencingCourt", { it.variable("sentencingCourt") }, equalTo("High Court")),
        has("sentencingDate", { it.variable("sentencingDate") }, equalTo("01 Oct 2020")),
        has("sentenceExpiryDate", { it.variable("sentenceExpiryDate") }, equalTo("29 Oct 2020")),
        has("vulnerabilityDiversityDetail", { it.variable("vulnerabilityDiversityDetail") }, equalTo("Some stuff")),
        has("hasContrabandDetail", { it.variable("hasContrabandDetail") }, equalTo("YES")),
        has("contrabandDetail", { it.variable("contrabandDetail") }, equalTo("Some contraband detail")),
      )
    )
  }

  @Test
  fun `generate recall summary HTML with optional values not populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("recall-summary", capture(contextSlot)) } returns expectedHtml

    val result = underTest.generateHtml(
      RecallSummaryContext(
        ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.of(19, 32), ZoneId.of("Europe/London")),
        PersonName("Bertie", "Basset", "Badger"),
        LocalDate.of(1995, 10, 3),
        "croNumber",
        PersonName("Maria", lastName = "Badger"),
        Email("maria@thebadgers.set"),
        PhoneNumber("09876543210"),
        LEVEL_3,
        SentenceLength(2, 3, 10),
        "Some offence",
        "High Court",
        LocalDate.of(2020, 10, 1),
        LocalDate.of(2020, 10, 29),
        "Mr Probation Officer",
        "01234567890",
        PS_TOWER_HAMLETS,
        "Bryan Badger",
        "B1234",
        NomsNumber("AA1234A"),
        LocalDate.of(2020, 10, 1),
        setOf(POOR_BEHAVIOUR_FURTHER_OFFENCE),
        "London",
        false,
        null,
        false,
        null,
        PrisonName("Current Prison"),
        PrisonName("Last Release Prison")
      ),
      3
    )

    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("logoFileName", { it.variable("logoFileName") }, equalTo(HmppsLogo.fileName)),
        has("createdDate", { it.variable("createdDate") }, equalTo("01 Sep 2021")),
        has("createdTime", { it.variable("createdTime") }, equalTo("19:32")),
        has("recallNotificationTotalNumberOfPages", { it.variable("recallNotificationTotalNumberOfPages") }, equalTo("3")),
        has("assessedByUserName", { it.variable("assessedByUserName") }, equalTo("Maria Badger")),
        has("assessedByUserEmail", { it.variable("assessedByUserEmail") }, equalTo("maria@thebadgers.set")),
        has("assessedByUserPhoneNumber", { it.variable("assessedByUserPhoneNumber") }, equalTo("09876543210")),
        has("mappaLevel1", { it.variable("mappaLevel1") }, equalTo("false")),
        has("mappaLevel2", { it.variable("mappaLevel2") }, equalTo("false")),
        has("mappaLevel3", { it.variable("mappaLevel3") }, equalTo("true")),
        has("firstAndMiddleNames", { it.variable("firstAndMiddleNames") }, equalTo("Bertie Basset")),
        has("lastName", { it.variable("lastName") }, equalTo("Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("03 Oct 1995")),
        has("previousConvictionMainName", { it.variable("previousConvictionMainName") }, equalTo("Bryan Badger")),
        has("bookingNumber", { it.variable("bookingNumber") }, equalTo("B1234")),
        has("nomsNumber", { it.variable("nomsNumber") }, equalTo("AA1234A")),
        has("lastReleasePrisonName", { it.variable("lastReleasePrisonName") }, equalTo("Last Release Prison")),
        has("localDeliveryUnit", { it.variable("localDeliveryUnit") }, equalTo("PS - Tower Hamlets")),
        has("lastReleaseDate", { it.variable("lastReleaseDate") }, equalTo("01 Oct 2020")),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo("2 years 3 months 10 days")),
        has("indexOffence", { it.variable("indexOffence") }, equalTo("Some offence")),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("true")),
        has("croNumber", { it.variable("croNumber") }, equalTo("croNumber")),
        has("probationOfficerName", { it.variable("probationOfficerName") }, equalTo("Mr Probation Officer")),
        has("probationOfficerPhoneNumber", { it.variable("probationOfficerPhoneNumber") }, equalTo("01234567890")),
        has("localPoliceForce", { it.variable("localPoliceForce") }, equalTo("London")),
        has("currentPrisonName", { it.variable("currentPrisonName") }, equalTo("Current Prison")),
        has("sentencingCourt", { it.variable("sentencingCourt") }, equalTo("High Court")),
        has("sentencingDate", { it.variable("sentencingDate") }, equalTo("01 Oct 2020")),
        has("sentenceExpiryDate", { it.variable("sentenceExpiryDate") }, equalTo("29 Oct 2020")),
        has("vulnerabilityDiversityDetail", { it.variable("vulnerabilityDiversityDetail") }, equalTo("None")),
        has("hasContrabandDetail", { it.variable("hasContrabandDetail") }, equalTo("NO")),
        has("contrabandDetail", { it.variable("contrabandDetail") }, equalTo(null)),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
