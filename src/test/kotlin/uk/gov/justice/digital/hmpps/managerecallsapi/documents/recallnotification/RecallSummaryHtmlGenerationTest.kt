package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_TOWER_HAMLETS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecallSummaryHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = RecallSummaryGenerator(templateEngine)

  @Test
  fun `generate in custody recall summary HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        recallSummaryContext(true),
        3
      )
    )
  }

  @Test
  fun `generate not in custody recall summary HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        recallSummaryContext(false),
        3
      )
    )
  }

  private fun recallSummaryContext(inCustody: Boolean) = RecallSummaryContext(
    ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.of(17, 48), ZoneId.of("Europe/London")),
    FullName("Bertie Badger"),
    LocalDate.of(1995, 10, 3),
    CroNumber("croNumber"),
    PersonName("Maria", lastName = "Badger"),
    Email("maria@thebadgers.set"),
    PhoneNumber("09876543210"),
    MappaLevel.LEVEL_3,
    SentenceLength(2, 3, 10),
    "Some offence",
    CourtName("High Court"),
    LocalDate.of(2020, 10, 1),
    LocalDate.of(2020, 10, 29),
    FullName("Mr Probation Officer"),
    PhoneNumber("01234567890"),
    PS_TOWER_HAMLETS,
    FullName("Bryan Badger"),
    BookingNumber("B1234"),
    NomsNumber("AA1234A"),
    LocalDate.of(2020, 10, 1),
    setOf(ELM_FURTHER_OFFENCE),
    PoliceForceName("Devon & Cornwall Police"),
    inCustody,
    "I believe that they will bring contraband to prison",
    inCustody,
    "Some stuff",
    PrisonName("Current Prison"),
    PrisonName("Last Release Prison"),
    inCustody,
    !inCustody,
    if (!inCustody) "Some arrest issues" else null,
    if (!inCustody) "No Fixed Abode" else null
  )
}
