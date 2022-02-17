package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_BREACH_NON_CURFEW_CONDITION
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_EQUIPMENT_TAMPER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallReasonAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReasonAuditRepository
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallReasonAuditRepositoryIntegrationTest(
  @Qualifier("jpaRecallReasonAuditRepository") @Autowired private val jpaRepository: JpaRecallReasonAuditRepository,
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRecallRepository: JpaRecallRepository,
) : IntegrationTestBase() {
  private val underTest = RecallReasonAuditRepository(jpaRepository)

  @MockkBean
  private lateinit var fixedClock: Clock

  @BeforeEach
  fun `set up clock`() {
    every { fixedClock.instant() } returns Instant.parse("2021-10-04T13:15:50.00Z")
    every { fixedClock.zone } returns ZoneId.of("UTC")
  }

  @Test
  @Transactional
  fun `can get audit info for single field updated recall`() {
    recallRepository.save(recall, currentUserId)

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value, lastUpdatedDateTime = OffsetDateTime.now(fixedClock))))

    val lastUpdatedDateTime1 = OffsetDateTime.now()
    every { fixedClock.instant() } returns lastUpdatedDateTime1.toInstant()
    val recallToUpdate1 = recall.copy(reasonsForRecall = setOf(ELM_FURTHER_OFFENCE, ELM_EQUIPMENT_TAMPER, OTHER), lastUpdatedDateTime = lastUpdatedDateTime1)
    recallRepository.save(recallToUpdate1, currentUserId)

    val recallReasonAudits1 = underTest.auditDetailsForRecallId(recall.id)
    assertThat(recallReasonAudits1.size, equalTo(1))
    assertOffsetDateTimeNearEqual(recallReasonAudits1[0].updatedDateTime, lastUpdatedDateTime1)
    val recallReasonActual1 = Json.decodeFromString<Array<String>>(recallReasonAudits1[0].updatedValue)
    assertThat(recallReasonActual1.size, equalTo(3))
    assert(recallReasonActual1.contains("OTHER"))
    assert(recallReasonActual1.contains("ELM_FURTHER_OFFENCE"))
    assert(recallReasonActual1.contains("ELM_EQUIPMENT_TAMPER"))

    val lastUpdatedDateTime2 = OffsetDateTime.now()
    every { fixedClock.instant() } returns lastUpdatedDateTime2.toInstant()
    val recallToUpdate2 = recall.copy(reasonsForRecall = setOf(ELM_BREACH_NON_CURFEW_CONDITION), lastUpdatedDateTime = lastUpdatedDateTime2)
    recallRepository.save(recallToUpdate2, currentUserId)

    val recallReasonAudits2 = underTest.auditDetailsForRecallId(recall.id)
    assertThat(recallReasonAudits2.size, equalTo(2))
    val latestAudit = recallReasonAudits2.maxByOrNull { it.auditId }
    assertOffsetDateTimeNearEqual(latestAudit!!.updatedDateTime, lastUpdatedDateTime2)
    val recallReasonActual2 = Json.decodeFromString<Array<String>>(latestAudit.updatedValue)

    assertThat(recallReasonActual2.size, equalTo(1))
    assert(recallReasonActual2.contains("ELM_BREACH_NON_CURFEW_CONDITION"))
  }

  @Test
  @Transactional
  fun `can get audit summary for updated recall`() {
    recallRepository.saveAndFlush(recall.copy(lastUpdatedByUserId = currentUserId.value))

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))

    val lastUpdatedDateTime1 = OffsetDateTime.now()
    val recallToUpdate = recall.copy(lastUpdatedDateTime = lastUpdatedDateTime1, reasonsForRecall = setOf(ELM_FURTHER_OFFENCE, ELM_EQUIPMENT_TAMPER))
    recallRepository.saveAndFlush(recallToUpdate)

    val recallReasonAudits1 = underTest.auditSummaryForRecallId(recall.id)
    assertThat(recallReasonAudits1!!.columnName, equalTo("reasons_for_recall"))
    assertThat(recallReasonAudits1.auditCount, equalTo(1))
    assertThat(recallReasonAudits1.updatedByUserName, equalTo("Test User"))
    assertOffsetDateTimeNearEqual(recallReasonAudits1.updatedDateTime, lastUpdatedDateTime1)

    val lastUpdatedDateTime2 = OffsetDateTime.now()
    val recallToUpdate2 = recall.copy(lastUpdatedDateTime = lastUpdatedDateTime2, reasonsForRecall = setOf(ELM_FURTHER_OFFENCE))
    recallRepository.saveAndFlush(recallToUpdate2)

    val recallReasonAudits2 = underTest.auditSummaryForRecallId(recall.id)
    assertThat(recallReasonAudits2!!.columnName, equalTo("reasons_for_recall"))
    assertThat(recallReasonAudits2.auditCount, equalTo(2))
    assertThat(recallReasonAudits2.updatedByUserName, equalTo("Test User"))
    assertOffsetDateTimeNearEqual(recallReasonAudits2.updatedDateTime, lastUpdatedDateTime2)
  }

  private fun assertOffsetDateTimeNearEqual(
    actual: Timestamp,
    expected: OffsetDateTime
  ) {
    // Due to differences in rounding (trigger drops last 0 on nano-seconds) we need to allow some variance on OffsetDateTimes
    Assertions.assertThat(actual.toLocalDateTime().atOffset(ZoneOffset.UTC)).isCloseTo(expected, Assertions.within(1, ChronoUnit.MILLIS))!!
  }
}
