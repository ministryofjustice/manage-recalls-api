package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_EQUIPMENT_TAMPER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallReasonAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReasonAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallReasonAuditRepositoryIntegrationTest(
  @Qualifier("jpaRecallReasonAuditRepository") @Autowired private val jpaRepository: JpaRecallReasonAuditRepository,
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRecallRepository: JpaRecallRepository,
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  private val nomsNumber = randomNoms()
  private val createdByUserId = ::UserId.random()
  private val recallId = ::RecallId.random()
  private val now = OffsetDateTime.now()
  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Barrie"), null, LastName("Badger"))
  private val currentUserId = ::UserId.random()

  private val underTest = RecallReasonAuditRepository(jpaRepository)
  private val recallRepository = RecallRepository(jpaRecallRepository)

  @BeforeEach
  fun `setup createdBy user`() {
    createUserDetails(currentUserId)
    createUserDetails(createdByUserId)
  }

  private fun createUserDetails(userId: UserId) {
    userDetailsRepository.save(
      UserDetails(
        userId,
        FirstName("Test"),
        LastName("User"),
        "",
        Email("test@user.com"),
        PhoneNumber("09876543210"),
        CaseworkerBand.FOUR_PLUS,
        OffsetDateTime.now()
      )
    )
  }

  @Test
  @Transactional
  fun `can get audit info for single field updated recall`() {
    recallRepository.save(recall, currentUserId)

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))

    val recallToUpdate = recall.copy(reasonsForRecall = setOf(ELM_FURTHER_OFFENCE, ELM_EQUIPMENT_TAMPER, OTHER))
    recallRepository.save(recallToUpdate, currentUserId)

    val recallReasonAudits = underTest.recallReasonAuditForRecallId(recall.id)
    assertThat(recallReasonAudits.size, equalTo(1))
    val recallReasonActual = Json.decodeFromString<Array<String>>(recallReasonAudits[0].updatedValue)

    assertThat(recallReasonActual.size, equalTo(3))
    assert(recallReasonActual.contains("OTHER"))
    assert(recallReasonActual.contains("ELM_FURTHER_OFFENCE"))
    assert(recallReasonActual.contains("ELM_EQUIPMENT_TAMPER"))
  }

  @Test
  @org.springframework.transaction.annotation.Transactional
  fun `can get audit summary for updated recall`() {
    assertThat(underTest.count(), equalTo(0))
    recallRepository.saveAndFlush(recall.copy(lastUpdatedByUserId = currentUserId.value))

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))

    val lastUpdatedDateTime1 = OffsetDateTime.now()
    val recallToUpdate = recall.copy(lastUpdatedDateTime = lastUpdatedDateTime1, reasonsForRecall = setOf(ELM_FURTHER_OFFENCE, ELM_EQUIPMENT_TAMPER))
    recallRepository.saveAndFlush(recallToUpdate)

    val lastUpdatedDateTime2 = OffsetDateTime.now()
    val recallToUpdate2 = recall.copy(lastUpdatedDateTime = lastUpdatedDateTime2, reasonsForRecall = setOf(ELM_FURTHER_OFFENCE))
    recallRepository.saveAndFlush(recallToUpdate2)

    assertThat(underTest.count(), equalTo(3))
    val recallReasonAudits = underTest.auditSummaryForRecallId(recall.id)
    assertThat(recallReasonAudits!!.columnName, equalTo("reasons_for_recall"))
    assertThat(recallReasonAudits.auditCount, equalTo(2))
    // Due to differences in rounding (trigger drops last 0 on nano-seconds) we need to allow some variance on OffsetDateTimes
    Assertions.assertThat(recallReasonAudits.updatedDateTime.toLocalDateTime().atOffset(ZoneOffset.UTC))
      .isCloseTo(lastUpdatedDateTime2, Assertions.within(1, ChronoUnit.MILLIS))!!
  }
}
