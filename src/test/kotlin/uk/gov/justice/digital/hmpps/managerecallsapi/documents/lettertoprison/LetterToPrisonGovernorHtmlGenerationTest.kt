package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LetterToPrisonGovernorHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T16:48:30.00Z"), ZoneId.of("UTC"))
  private val underTest = LetterToPrisonGovernorGenerator(templateEngine, fixedClock)

  @Test
  fun `generate fully populated HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(
            ::RecallId.random(), NomsNumber("AA1234A"),
            recallLength = RecallLength.FOURTEEN_DAYS,
            bookingNumber = "B1234",
            lastReleaseDate = LocalDate.of(2020, 10, 1)
          ),
          Prisoner(firstName = "Billie", middleNames = "Bob", lastName = "Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          UserDetails(::UserId.random(), FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"))
        )
      )
    )
  }
}
