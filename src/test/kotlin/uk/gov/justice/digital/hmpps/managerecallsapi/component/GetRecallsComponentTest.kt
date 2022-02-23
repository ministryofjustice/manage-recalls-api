package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_MIDDLE_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponseLite
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Movement
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate
import java.time.LocalTime

class GetRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `get all recalls`() {
    val nomsNumber1 = NomsNumber("123456")
    val nomsNumber2 = NomsNumber("987654")
    val nomsNumber3 = NomsNumber("AB1234Z")
    val nomsNumber4 = NomsNumber("ZY9876A")
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(nomsNumber3, Prisoner(nomsNumber3.value, status = "ACTIVE IN"))
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(nomsNumber4, Prisoner(nomsNumber4.value, status = "Something else"))
    prisonApiMockServer.latestMovementsRespondsWith(setOf(nomsNumber3), listOf(Movement(nomsNumber3.value, LocalDate.now(), LocalTime.now().minusHours(1))))

    val createdByUserId = authenticatedClient.userId

    val request1 = BookRecallRequest(
      nomsNumber1,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val request2 = BookRecallRequest(
      nomsNumber2,
      FirstName("Barrie"),
      MiddleNames("Barnie"),
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val request3 = BookRecallRequest(
      nomsNumber3,
      FirstName("Mary"),
      null,
      LastName("Mouse"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val request4 = BookRecallRequest(
      nomsNumber4,
      FirstName("Mary"),
      null,
      LastName("Mouse"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )

    val recall1 = authenticatedClient.bookRecall(request1)
    val recall2 = authenticatedClient.bookRecall(request2)
    authenticatedClient.updateRecall(
      recall2.recallId,
      UpdateRecallRequest(
        licenceNameCategory = FIRST_MIDDLE_LAST
      )
    )
    val rtcRecall = authenticatedClient.bookRecall(request3)
    authenticatedClient.updateRecall(
      rtcRecall.recallId,
      UpdateRecallRequest(
        assessedByUserId = createdByUserId,
        inCustodyAtAssessment = false,
        warrantReferenceNumber = WarrantReferenceNumber("ABC1234/C")
      )
    )
    val nicRecall = authenticatedClient.bookRecall(request4)
    authenticatedClient.updateRecall(
      nicRecall.recallId,
      UpdateRecallRequest(
        assessedByUserId = createdByUserId,
        inCustodyAtAssessment = false,
        warrantReferenceNumber = WarrantReferenceNumber("ABC1234/C")
      )
    )

    val response = authenticatedClient.getAllRecalls()

    assertThat(
      response, List<RecallResponseLite>::containsAll,
      listOf(
        RecallResponseLite(
          recall1.recallId,
          recall1.nomsNumber,
          createdByUserId,
          fixedClockTime,
          fixedClockTime,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          FIRST_LAST,
          Status.BEING_BOOKED_ON
        ),
        RecallResponseLite(
          recall2.recallId,
          recall2.nomsNumber,
          createdByUserId,
          fixedClockTime,
          fixedClockTime,
          FirstName("Barrie"),
          MiddleNames("Barnie"),
          LastName("Badger"),
          FIRST_MIDDLE_LAST,
          Status.BEING_BOOKED_ON
        ),
        RecallResponseLite(
          rtcRecall.recallId,
          rtcRecall.nomsNumber,
          createdByUserId,
          fixedClockTime,
          fixedClockTime,
          FirstName("Mary"),
          null,
          LastName("Mouse"),
          FIRST_LAST,
          Status.AWAITING_DOSSIER_CREATION,
          dossierTargetDate = LocalDate.of(2022, 2, 7),
          inCustodyAtAssessment = false
        ),
        RecallResponseLite(
          nicRecall.recallId,
          nicRecall.nomsNumber,
          createdByUserId,
          fixedClockTime,
          fixedClockTime,
          FirstName("Mary"),
          null,
          LastName("Mouse"),
          FIRST_LAST,
          Status.AWAITING_RETURN_TO_CUSTODY,
          inCustodyAtAssessment = false
        )
      )
    )
  }
}
