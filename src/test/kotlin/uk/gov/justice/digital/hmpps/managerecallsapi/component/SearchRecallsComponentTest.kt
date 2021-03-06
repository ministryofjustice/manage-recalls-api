package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class SearchRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `search recalls by nomsNumber`() {
    val nomsNumberToSearch = randomNoms()
    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))
    val createdByUserId = authenticatedClient.userId
    val recall1 = Recall(
      ::RecallId.random(),
      randomNoms(),
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
      nomsNumberToSearch,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val recall3 = Recall(
      ::RecallId.random(),
      nomsNumberToSearch,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val recall4 = Recall(
      ::RecallId.random(),
      randomNoms(),
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    recallRepository.saveAll(listOf(recall1, recall2, recall3, recall4))

    val response = authenticatedClient.searchRecalls(RecallSearchRequest(nomsNumberToSearch))

    assertThat(
      response, List<RecallResponse>::equals,
      listOf(
        RecallResponse(
          recall2.recallId(),
          nomsNumberToSearch,
          createdByUserId,
          now,
          now,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          Status.BEING_BOOKED_ON,
        ),
        RecallResponse(
          recall3.recallId(),
          nomsNumberToSearch,
          createdByUserId,
          now,
          now,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          Status.BEING_BOOKED_ON,
        )
      )
    )
  }
}
