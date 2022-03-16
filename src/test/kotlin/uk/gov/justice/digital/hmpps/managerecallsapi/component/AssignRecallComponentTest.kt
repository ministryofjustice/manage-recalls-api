package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class AssignRecallComponentTest : ComponentTestBase() {

  private val zone = ZoneId.of("UTC")
  private val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), zone)

  @Test
  fun `assign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val createdByUserId = ::UserId.random()
    setupUserDetailsFor(assignee)
    setupUserDetailsFor(createdByUserId)

    val recall = Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )

    recallRepository.save(recall, createdByUserId)
    userDetailsRepository.save(
      UserDetails(
        assignee,
        FirstName("Bertie"),
        LastName("Badger"),
        "",
        Email("b@b.com"),
        PhoneNumber("0987654321"),
        CaseworkerBand.FOUR_PLUS,
        OffsetDateTime.now()
      )
    )

    val response = authenticatedClient.assignRecall(recallId, assignee)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          OffsetDateTime.now(fixedClock), FirstName("Barrie"), null, LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          Status.BEING_BOOKED_ON,
          assignee = assignee,
          assigneeUserName = FullName("Bertie Badger"),
        )
      )
    )
  }

  @Test
  fun `unassign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val notAssignee = ::UserId.random()
    val createdByUserId = ::UserId.random()
    setupUserDetailsFor(assignee)
    setupUserDetailsFor(createdByUserId)

    val recall = Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
      assignee = assignee
    )
    recallRepository.save(recall, createdByUserId)

    val response = authenticatedClient.unassignRecall(recallId, notAssignee)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          OffsetDateTime.now(fixedClock), FirstName("Barrie"), null, LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          Status.BEING_BOOKED_ON
        )
      )
    )
  }

  @Test
  fun `must pass a valid uuid as assignee`() {
    val recallId = ::RecallId.random()

    authenticatedClient.delete("/recalls/$recallId/assignee/notauuid", INTERNAL_SERVER_ERROR)
    authenticatedClient.post("/recalls/$recallId/assignee/notauuid", "").expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
  }
}
