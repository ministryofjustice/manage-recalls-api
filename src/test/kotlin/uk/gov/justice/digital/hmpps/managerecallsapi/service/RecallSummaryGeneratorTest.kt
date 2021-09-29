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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecallSummaryGeneratorTest {
  private val templateEngine = mockk<SpringTemplateEngine>()
  private val fixedClock = Clock.fixed(Instant.parse("2021-09-01T18:32:00.00Z"), ZoneId.of("UTC"))

  private val underTest = RecallSummaryGenerator(templateEngine, fixedClock)

  @Test
  fun `generate recall summary HTML with all values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("recall-summary", capture(contextSlot)) } returns expectedHtml

    val nomsNumber: NomsNumber = randomNoms()
    val assessedByUserId = ::UserId.random()
    val result = underTest.generateHtml(
      RecallSummaryContext(
        Recall(
          ::RecallId.random(),
          nomsNumber,
          mappaLevel = MappaLevel.LEVEL_3,
          contrabandDetail = "Some contraband detail",
          previousConvictionMainName = "Bryan Badger",
          bookingNumber = "B1234",
          lastReleaseDate = LocalDate.of(2020, 10, 1),
          reasonsForRecall = setOf(
            ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
          ),
          sentencingInfo = SentencingInfo(
            LocalDate.of(2020, 10, 1),
            LocalDate.of(2020, 11, 1),
            LocalDate.of(2020, 10, 29),
            "High Court",
            "Some offence",
            SentenceLength(2, 3, 10),
          ),
          probationInfo = ProbationInfo(
            "Mr Probation Officer",
            "01234567890",
            "officer@myprobation.com",
            LocalDeliveryUnit.PS_BARKING_DAGENHAM_HAVERING,
            "Ms Authoriser"
          ),
          localPoliceForce = "London",
          vulnerabilityDiversityDetail = "Some stuff",
          assessedByUserId = assessedByUserId
        ),
        Prisoner(
          firstName = "Bertie",
          middleNames = "Basset",
          lastName = "Badger",
          dateOfBirth = LocalDate.of(1995, 10, 3),
          bookNumber = "bookNumber",
          croNumber = "croNumber"
        ),
        "Prison 1",
        "Prison 2",
        UserDetails(assessedByUserId, FirstName("Maria"), LastName("Badger"), "", Email("maria@thebadgers.set"), PhoneNumber("09876543210")),
        3
      )
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
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo(nomsNumber.value)),
        has("releasingPrison", { it.variable("releasingPrison") }, equalTo("Prison 1")),
        has("releaseDate", { it.variable("releaseDate") }, equalTo("01 Oct 2020")),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo("2 years 3 months 10 days")),
        has("indexOffence", { it.variable("indexOffence") }, equalTo("Some offence")),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("true")),
        has("pncCroNumber", { it.variable("pncCroNumber") }, equalTo("croNumber")),
        has("offenderManagerName", { it.variable("offenderManagerName") }, equalTo("Mr Probation Officer")),
        has("offenderManagerContactNumber", { it.variable("offenderManagerContactNumber") }, equalTo("01234567890")),
        has("policeSpoc", { it.variable("policeSpoc") }, equalTo("London")),
        has("currentPrison", { it.variable("currentPrison") }, equalTo("Prison 2")),
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
  fun `generate recall summary HTML with no values populated`() {
    val expectedHtml = "expected HTML"
    val contextSlot = slot<IContext>()

    every { templateEngine.process("recall-summary", capture(contextSlot)) } returns expectedHtml

    val nomsNumber = randomNoms()
    val result = underTest.generateHtml(
      RecallSummaryContext(
        Recall(::RecallId.random(), nomsNumber),
        Prisoner(),
        "",
        "",
        UserDetails(::UserId.random(), FirstName("A"), LastName("B"), "", Email("C"), PhoneNumber("D")),
        OTHER_PAGES_IN_RECALL_NOTIFICATION
      )
    )
    assertThat(result, equalTo(expectedHtml))
    assertThat(
      contextSlot.captured,
      allOf(
        has("createdDate", { it.variable("createdDate") }, equalTo("01 Sep 2021")),
        has("createdTime", { it.variable("createdTime") }, equalTo("19:32")),

        has("caseworkerName", { it.variable("caseworkerName") }, equalTo("A B")),
        has("caseworkerEmail", { it.variable("caseworkerEmail") }, equalTo("C")),
        has("caseworkerPhoneNumber", { it.variable("caseworkerPhoneNumber") }, equalTo("D")),

        has("mappaLevel1", { it.variable("mappaLevel1") }, equalTo("false")),
        has("mappaLevel2", { it.variable("mappaLevel2") }, equalTo("false")),
        has("mappaLevel3", { it.variable("mappaLevel3") }, equalTo("false")),
        has("forename", { it.variable("forename") }, equalTo("")),
        has("surname", { it.variable("surname") }, equalTo(null)),
        has("dateOfBirth", { it.variable("dateOfBirth") }, equalTo(null)),
        has("policeFileName", { it.variable("policeFileName") }, equalTo(null)),
        has("prisonNumber", { it.variable("prisonNumber") }, equalTo(null)),
        has("pnomisNumber", { it.variable("pnomisNumber") }, equalTo(nomsNumber.value)),
        has("releasingPrison", { it.variable("releasingPrison") }, equalTo("")),
        has("releaseDate", { it.variable("releaseDate") }, equalTo(null)),
        has("lengthOfSentence", { it.variable("lengthOfSentence") }, equalTo(null)),
        has("indexOffence", { it.variable("indexOffence") }, equalTo(null)),
        has("furtherCharge", { it.variable("furtherCharge") }, equalTo("false")),
        has("pncCroNumber", { it.variable("pncCroNumber") }, equalTo(null)),
        has("offenderManagerName", { it.variable("offenderManagerName") }, equalTo(null)),
        has("offenderManagerContactNumber", { it.variable("offenderManagerContactNumber") }, equalTo(null)),
        has("policeSpoc", { it.variable("policeSpoc") }, equalTo(null)),
        has("currentPrison", { it.variable("currentPrison") }, equalTo("")),
        has("sentencingCourt", { it.variable("sentencingCourt") }, equalTo(null)),
        has("sentencingDate", { it.variable("sentencingDate") }, equalTo(null)),
        has("sed", { it.variable("sed") }, equalTo(null)),
        has("vulnerabilityDetail", { it.variable("vulnerabilityDetail") }, equalTo("None")),
        has("contraband", { it.variable("contraband") }, equalTo("NO")),
        has("contrabandDetail", { it.variable("contrabandDetail") }, equalTo(null)),
      )
    )
  }

  private fun IContext.variable(variableName: String) = getVariable(variableName)?.toString()
}
