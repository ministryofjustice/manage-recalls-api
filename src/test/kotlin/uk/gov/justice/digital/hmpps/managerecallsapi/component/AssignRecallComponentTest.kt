package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.jvm.Throws

class AssignRecallComponentTest : ComponentTestBase() {
  @MockkBean
  private lateinit var fixedClock: Clock

  private val zone = ZoneId.of("UTC")
  private val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), zone)

  @BeforeEach
  fun `set up clock`() {
    every { fixedClock.instant() } returns Instant.parse("2021-10-04T13:15:50.00Z")
    every { fixedClock.zone } returns zone
  }

  @Test
  fun `assign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val createdByUserId = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, createdByUserId, now, now)
    recallRepository.save(recall)
    userDetailsRepository.save(
      UserDetails(
        assignee,
        FirstName("Bertie"),
        LastName("Badger"),
        "",
        Email("b@b.com"),
        PhoneNumber("0987654321"),
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
          OffsetDateTime.now(fixedClock),
          Status.BEING_BOOKED_ON,
          assignee = assignee,
          assigneeUserName = "Bertie Badger"
        )
      )
    )
  }

  @Test
  fun `unassign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val createdByUserId = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, createdByUserId, now, now, assignee = assignee)
    recallRepository.save(recall)

    val response = authenticatedClient.unassignRecall(recallId, assignee)

    assertThat(
      response, equalTo(RecallResponse(recallId, nomsNumber, createdByUserId, now, OffsetDateTime.now(fixedClock), Status.BEING_BOOKED_ON))
    )
  }

  @Test
  fun `unassign recall with wrong assignee throws 404`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val otherAssignee = ::UserId.random()
    val createdByUserId = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, createdByUserId, OffsetDateTime.now(), assignee = assignee)
    recallRepository.save(recall)

    authenticatedClient.unassignRecall(recallId, otherAssignee, HttpStatus.NOT_FOUND)
  }

  @Test
  @Throws(MethodArgumentTypeMismatchException::class)
  fun `must pass a valid uuid as assignee`() {
    val recallId = ::RecallId.random()

    authenticatedClient.delete("/recalls/$recallId/assignee/notauuid", INTERNAL_SERVER_ERROR)
    authenticatedClient.post("/recalls/$recallId/assignee/notauuid", "").expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
  }
}
