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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.FirstAndMiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
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
        FirstAndMiddleNames(FirstName("Bertie"), MiddleNames("Basset")),
        LastName("Badger"),
        LocalDate.of(1995, 10, 3),
        "croNumber",
        PersonName(FirstName("Maria"), null, LastName("Badger")),
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
        "Some stuff",
        "Some contraband detail",
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

        has("caseworkerName", { it.variable("caseworkerName") }, equalTo("Maria Badger")),
        has("caseworkerEmail", { it.variable("caseworkerEmail") }, equalTo("maria@thebadgers.set")),
        has("caseworkerPhoneNumber", { it.variable("caseworkerPhoneNumber") }, equalTo("09876543210")),

        has("mappaLevel1", { it.variable("mappaLevel1") }, equalTo("false")),
        has("mappaLevel2", { it.variable("mappaLevel2") }, equalTo("false")),
        has("mappaLevel3", { it.variable("mappaLevel3") }, equalTo("true")),
        has("forename", { it.variable("forename") }, equalTo("Bertie Basset")),
        has("surname", { it.variable("surname") }, equalTo("Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("03 Oct 1995")),
        has("policeFileName", { it.variable("policeFileName") }, equalTo("Bryan Badger")),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo("B1234")),
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo("AA1234A")),
        has("releasingPrison", { it.variable("releasingPrison") }, equalTo("Last Release Prison")),
        has("localDeliveryUnit", { it.variable("localDeliveryUnit") }, equalTo("PS - Barking, Dagenham & Havering")),
        has("releaseDate", { it.variable("releaseDate") }, equalTo("01 Oct 2020")),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo("2 years 3 months 10 days")),
        has("indexOffence", { it.variable("indexOffence") }, equalTo("Some offence")),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("true")),
        has("pncCroNumber", { it.variable("pncCroNumber") }, equalTo("croNumber")),
        has("offenderManagerName", { it.variable("offenderManagerName") }, equalTo("Mr Probation Officer")),
        has("offenderManagerContactNumber", { it.variable("offenderManagerContactNumber") }, equalTo("01234567890")),
        has("policeSpoc", { it.variable("policeSpoc") }, equalTo("London")),
        has("currentPrison", { it.variable("currentPrison") }, equalTo("Current Prison")),
        has("sentencingCourt", { it.variable("sentencingCourt") }, equalTo("High Court")),
        has("sentencingDate", { it.variable("sentencingDate") }, equalTo("01 Oct 2020")),
        has("sed", { it.variable("sed") }, equalTo("29 Oct 2020")),
        has("vulnerabilityDetail", { it.variable("vulnerabilityDetail") }, equalTo("Some stuff")),
        has("contraband", { it.variable("contraband") }, equalTo("YES")),
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
        FirstAndMiddleNames(FirstName("Bertie"), MiddleNames("Basset")),
        LastName("Badger"),
        LocalDate.of(1995, 10, 3),
        "croNumber",
        PersonName(FirstName("Maria"), null, LastName("Badger")),
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
        null,
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

        has("caseworkerName", { it.variable("caseworkerName") }, equalTo("Maria Badger")),
        has("caseworkerEmail", { it.variable("caseworkerEmail") }, equalTo("maria@thebadgers.set")),
        has("caseworkerPhoneNumber", { it.variable("caseworkerPhoneNumber") }, equalTo("09876543210")),

        has("mappaLevel1", { it.variable("mappaLevel1") }, equalTo("false")),
        has("mappaLevel2", { it.variable("mappaLevel2") }, equalTo("false")),
        has("mappaLevel3", { it.variable("mappaLevel3") }, equalTo("true")),
        has("forename", { it.variable("forename") }, equalTo("Bertie Basset")),
        has("surname", { it.variable("surname") }, equalTo("Badger")),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo("03 Oct 1995")),
        has("policeFileName", { it.variable("policeFileName") }, equalTo("Bryan Badger")),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo("B1234")),
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo("AA1234A")),
        has("releasingPrison", { it.variable("releasingPrison") }, equalTo("Last Release Prison")),
        has("localDeliveryUnit", { it.variable("localDeliveryUnit") }, equalTo("PS - Tower Hamlets")),
        has("releaseDate", { it.variable("releaseDate") }, equalTo("01 Oct 2020")),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo("2 years 3 months 10 days")),
        has("indexOffence", { it.variable("indexOffence") }, equalTo("Some offence")),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("true")),
        has("pncCroNumber", { it.variable("pncCroNumber") }, equalTo("croNumber")),
        has("offenderManagerName", { it.variable("offenderManagerName") }, equalTo("Mr Probation Officer")),
        has("offenderManagerContactNumber", { it.variable("offenderManagerContactNumber") }, equalTo("01234567890")),
        has("policeSpoc", { it.variable("policeSpoc") }, equalTo("London")),
        has("currentPrison", { it.variable("currentPrison") }, equalTo("Current Prison")),
        has("sentencingCourt", { it.variable("sentencingCourt") }, equalTo("High Court")),
        has("sentencingDate", { it.variable("sentencingDate") }, equalTo("01 Oct 2020")),
        has("sed", { it.variable("sed") }, equalTo("29 Oct 2020")),
        has("vulnerabilityDetail", { it.variable("vulnerabilityDetail") }, equalTo("None")),
        has("contraband", { it.variable("contraband") }, equalTo("NO")),
        has("contrabandDetail", { it.variable("contrabandDetail") }, equalTo(null)),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
