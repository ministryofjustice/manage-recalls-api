package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RecallSummaryHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val fixedClock = Clock.fixed(Instant.parse("2021-09-01T16:48:30.00Z"), ZoneId.of("UTC"))
  private val underTest = RecallSummaryGenerator(templateEngine, fixedClock)

  @Test
  fun `generate recall summary HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        RecallSummaryContext(
          Recall(
            RecallId(UUID.randomUUID()), NomsNumber("AA1234A"),
            contrabandDetail = "I believe that they will bring contraband to prison",
            mappaLevel = MappaLevel.LEVEL_3,
            previousConvictionMainName = "Bryan Badger",
            bookingNumber = "B1234",
            lastReleaseDate = LocalDate.of(2020, 10, 1),
            reasonsForRecall = setOf(
              ReasonForRecall.ELM_FURTHER_OFFENCE
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
              ProbationDivision.LONDON,
              "Ms Authoriser"
            ),
            localPoliceForce = "London",
            vulnerabilityDiversityDetail = "Some stuff"
          ),
          Prisoner(
            firstName = "Bertie",
            middleNames = "Basset",
            lastName = "Badger",
            dateOfBirth = LocalDate.of(1995, 10, 3),
            bookNumber = "bookNumber",
            croNumber = "croNumber"
          ),
          "Prison A",
          "Prison B"
        )
      )
    )
  }
}
