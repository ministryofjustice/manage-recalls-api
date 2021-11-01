package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
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
import java.time.OffsetDateTime
import java.time.ZoneId

class LetterToPrisonCustodyOfficeHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T16:48:30.00Z"), ZoneId.of("UTC"))
  private val underTest = LetterToPrisonCustodyOfficeGenerator(templateEngine, fixedClock)

  @Test
  fun `generate fully populated HTML FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(
            ::RecallId.random(), NomsNumber("AA1234A"), ::UserId.random(), OffsetDateTime.now(),
            recallLength = recallLength,
            contraband = true,
            contrabandDetail = "Because...",
            vulnerabilityDiversity = true,
            vulnerabilityDiversityDetail = "Yes, yadda yadda",
            mappaLevel = MappaLevel.LEVEL_2,
            bookingNumber = "B1234",
            additionalLicenceConditions = true,
            additionalLicenceConditionsDetail = "Blah blah blah",
            differentNomsNumber = true,
            differentNomsNumberDetail = "ABC1234F",
          ),
          Prisoner(firstName = "Billie", lastName = "Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(), FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"),
            OffsetDateTime.now()
          )
        )
      )
    )
  }

  @Test
  fun `generate fully populated HTML FTR 28 days`(approver: ContentApprover) {
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(
            ::RecallId.random(), NomsNumber("AA1234A"), ::UserId.random(), OffsetDateTime.now(),
            recallLength = recallLength,
            bookingNumber = "B1234",
            differentNomsNumber = true,
            differentNomsNumberDetail = "ABC1234F",
            mappaLevel = MappaLevel.LEVEL_2,
            additionalLicenceConditions = true,
            additionalLicenceConditionsDetail = "Blah blah blah",
            contraband = true,
            contrabandDetail = "Because...",
            vulnerabilityDiversity = true,
            vulnerabilityDiversityDetail = "Yes, yadda yadda",
          ),
          Prisoner(firstName = "Billie", lastName = "Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(), FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"),
            OffsetDateTime.now()
          )
        )
      )
    )
  }

  @Test
  fun `generate html when no additional license conditions FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(
            ::RecallId.random(), NomsNumber("AA1234A"), ::UserId.random(), OffsetDateTime.now(),
            recallLength = recallLength,
            contraband = true,
            contrabandDetail = "Because...",
            vulnerabilityDiversity = true,
            vulnerabilityDiversityDetail = "Yes, yadda yadda",
            mappaLevel = MappaLevel.LEVEL_2,
            bookingNumber = "B1234",
            additionalLicenceConditions = false,
            differentNomsNumber = true,
            differentNomsNumberDetail = "ABC1234F",
          ),
          Prisoner(firstName = "Billie", lastName = "Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(), FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"),
            OffsetDateTime.now()
          )
        )
      )
    )
  }
}
