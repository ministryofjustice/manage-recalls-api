package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallAuditRepository
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallAuditRepositoryIntegrationTest(
  @Qualifier("jpaRecallAuditRepository") @Autowired private val jpaRepository: JpaRecallAuditRepository,
) : IntegrationTestBase() {
  private val underTest = RecallAuditRepository(jpaRepository)

  @MockkBean
  private lateinit var fixedClock: Clock

  @BeforeEach
  fun `set up clock`() {
    every { fixedClock.instant() } returns OffsetDateTime.now().toInstant()
    every { fixedClock.zone } returns ZoneId.of("UTC")
  }

  @Test
  @Transactional
  fun `can get audit info for single field updated recall`() {
    recallRepository.save(recall, currentUserId)

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value, lastUpdatedDateTime = OffsetDateTime.now(fixedClock))))

    val recallToUpdate = recall.copy(contrabandDetail = "blah blah blah")
    recallRepository.save(recallToUpdate, currentUserId)

    val recallAudits = underTest.auditDetailsForRecallIdAndColumnName(recall.id, "contraband_detail")
    assertThat(recallAudits.size, equalTo(1))
    assertThat(recallAudits[0].updatedValue, equalTo("blah blah blah"))
  }

  @Test
  @Transactional
  fun `can get audit summary for updated recall`() {
    recallRepository.save(recall, currentUserId)

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value, lastUpdatedDateTime = OffsetDateTime.now(fixedClock))))

    val recallToUpdate = recall.copy(contrabandDetail = "blah blah blah")
    recallRepository.saveAndFlush(recallToUpdate)
    val recallToUpdate2 = recall.copy(contrabandDetail = "not blah blah blah")
    recallRepository.saveAndFlush(recallToUpdate2)

    val recallAudits = underTest.auditSummaryForRecall(recall.id)
    assertThat(recallAudits.map { it.columnName }, equalTo(listOf("last_updated_by_user_id", "licence_name_category", "date_of_birth", "last_updated_date_time", "contraband_detail", "noms_number", "created_by_user_id", "cro_number", "created_date_time", "first_name", "last_name")))
    val contrabandDetailAudit = recallAudits.first { it.columnName == "contraband_detail" }
    assertThat(contrabandDetailAudit.auditCount, equalTo(2))
  }
}
