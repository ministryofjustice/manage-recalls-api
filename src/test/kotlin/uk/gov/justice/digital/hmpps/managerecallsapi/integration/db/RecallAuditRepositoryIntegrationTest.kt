package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallAuditRepository
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
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallAuditRepositoryIntegrationTest(
  @Qualifier("jpaRecallAuditRepository") @Autowired private val jpaRepository: JpaRecallAuditRepository,
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRecallRepository: JpaRecallRepository,
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  private val nomsNumber = randomNoms()
  private val createdByUserId = ::UserId.random()
  private val recallId = ::RecallId.random()
  private val now = OffsetDateTime.now()
  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Barrie"), null, LastName("Badger"))
  private val currentUserId = ::UserId.random()

  private val underTest = RecallAuditRepository(jpaRepository)
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

    val recallToUpdate = recall.copy(contrabandDetail = "blah blah blah")
    recallRepository.save(recallToUpdate, currentUserId)

    val recallAudits = underTest.auditForRecallIdAndColumnName(recall.id, "contraband_detail")
    assertThat(recallAudits.size, equalTo(1))
    assertThat(recallAudits[0].updatedValue, equalTo("blah blah blah"))
  }

  @Test
  @Transactional
  fun `can get audit summary for updated recall`() {
    recallRepository.save(recall, currentUserId)

    assertThat(recallRepository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))

    val recallToUpdate = recall.copy(contrabandDetail = "blah blah blah")
    recallRepository.saveAndFlush(recallToUpdate)
    val recallToUpdate2 = recall.copy(contrabandDetail = "not blah blah blah")
    recallRepository.saveAndFlush(recallToUpdate2)

    val recallAudits = underTest.auditSummaryForRecall(recall.id)
    assertThat(recallAudits.map { it.columnName }, equalTo(listOf("last_updated_by_user_id", "licence_name_category", "last_updated_date_time", "contraband_detail", "noms_number", "created_by_user_id", "created_date_time", "id", "first_name", "last_name")))
    assertThat(recallAudits[3].columnName, equalTo("contraband_detail"))
    assertThat(recallAudits[3].auditCount, equalTo(2))
  }
}
