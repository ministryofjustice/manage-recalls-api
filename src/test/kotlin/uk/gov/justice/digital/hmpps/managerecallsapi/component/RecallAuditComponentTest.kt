package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallAuditService
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class RecallAuditComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Autowired private lateinit var recallAuditService: RecallAuditService

  @Test
  fun `get currentPrison audit for a recall that has been booked`() {
    val recallId = ::RecallId.random()
    val createdByUserId = authenticatedClient.userId

    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))

    val savedRecall = recallRepository.save(
      Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Brian"), null, LastName("Badgering"), currentPrison = PrisonId("ABC")),
      createdByUserId
    )

    val auditList = authenticatedClient.auditForField(recallId, "currentPrison")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo("ABC"))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(auditList[0].updatedDateTime, equalTo(savedRecall.lastUpdatedDateTime))
  }

  @Test
  fun `get contraband audit for a recall that has been booked`() {
    val recallId = ::RecallId.random()
    val createdByUserId = authenticatedClient.userId

    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))

    val savedRecall = recallRepository.save(
      Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Brian"), null, LastName("Badgering"), contraband = true),
      createdByUserId
    )

    val auditList = authenticatedClient.auditForField(recallId, "contraband")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(true))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(auditList[0].updatedDateTime, equalTo(savedRecall.lastUpdatedDateTime))
  }
}
