package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_MIDDLE_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class GetRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `get all recalls`() {
    val createdByUserId = authenticatedClient.userId

    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))
    val recall1 = Recall(
      ::RecallId.random(),
      NomsNumber("123456"),
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val recall2 = Recall(
      ::RecallId.random(),
      NomsNumber("987654"),
      createdByUserId,
      now,
      FirstName("Barrie"),
      MiddleNames("Barnie"),
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      licenceNameCategory = FIRST_MIDDLE_LAST
    )
    recallRepository.save(recall1, createdByUserId)
    recallRepository.save(recall2, createdByUserId)

    val response = authenticatedClient.getAllRecalls()

    assertThat(
      response, List<RecallResponse>::containsAll,
      listOf(
        RecallResponse(
          recall1.recallId(),
          recall1.nomsNumber,
          createdByUserId,
          now,
          now,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          FIRST_LAST,
          Status.BEING_BOOKED_ON
        ),
        RecallResponse(
          recall2.recallId(),
          recall2.nomsNumber,
          createdByUserId,
          now,
          now,
          FirstName("Barrie"),
          MiddleNames("Barnie"),
          LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          FIRST_MIDDLE_LAST,
          Status.BEING_BOOKED_ON
        )
      )
    )
  }
}
