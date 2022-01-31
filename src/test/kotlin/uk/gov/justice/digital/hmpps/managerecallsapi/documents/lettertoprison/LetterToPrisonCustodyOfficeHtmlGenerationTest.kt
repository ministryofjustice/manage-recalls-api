package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime

class LetterToPrisonCustodyOfficeHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonCustodyOfficeGenerator(templateEngine)

  @Test
  fun `generate fully populated HTML FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(
            ::RecallId.random(),
            NomsNumber("AA1234A"),
            ::UserId.random(),
            OffsetDateTime.now(),
            FirstName("Barrie"),
            null,
            LastName("Badger"),
            CroNumber("ABC/1234A"),
            LocalDate.of(1999, 12, 1),
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
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(),
            FirstName("Mandy"),
            LastName("Pandy"),
            "",
            Email("mandy@pandy.com"),
            PhoneNumber("09876543210"),
            CaseworkerBand.FOUR_PLUS,
            OffsetDateTime.now()
          ),
          LocalDate.of(2021, 10, 4)
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
            ::RecallId.random(),
            NomsNumber("AA1234A"),
            ::UserId.random(),
            OffsetDateTime.now(),
            FirstName("Barrie"),
            null,
            LastName("Badger"),
            CroNumber("ABC/1234A"),
            LocalDate.of(1999, 12, 1),
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
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(),
            FirstName("Mandy"),
            LastName("Pandy"),
            "",
            Email("mandy@pandy.com"),
            PhoneNumber("09876543210"),
            CaseworkerBand.FOUR_PLUS,
            OffsetDateTime.now()
          ),
          LocalDate.of(2021, 10, 4)
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
            ::RecallId.random(),
            NomsNumber("AA1234A"),
            ::UserId.random(),
            OffsetDateTime.now(),
            FirstName("Barrie"),
            null,
            LastName("Badger"),
            CroNumber("ABC/1234A"),
            LocalDate.of(1999, 12, 1),
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
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          PrisonName("Prison B"),
          RecallLengthDescription(recallLength),
          UserDetails(
            ::UserId.random(),
            FirstName("Mandy"),
            LastName("Pandy"),
            "",
            Email("mandy@pandy.com"),
            PhoneNumber("09876543210"),
            CaseworkerBand.FOUR_PLUS,
            OffsetDateTime.now()
          ),
          LocalDate.of(2021, 10, 4)
        )
      )
    )
  }
}
