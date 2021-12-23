package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaRecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallRepositoryIntegrationTest(
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRepository: JpaRecallRepository,
  @Qualifier("jpaDocumentRepository") @Autowired private val jpaDocumentRepository: JpaDocumentRepository,
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  private val nomsNumber = randomNoms()
  private val createdByUserId = ::UserId.random()
  private val recallId = ::RecallId.random()
  private val now = OffsetDateTime.now()
  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Barrie"), null, LastName("Badger"))
  private val details = "Some string"
  private val currentUserId = ::UserId.random()

  private val repository = RecallRepository(jpaRepository)
  private val documentRepository = DocumentRepository(jpaDocumentRepository)

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
  fun `saves and retrieves a recall with updated lastUpdatedByUserId`() {
    repository.save(recall, currentUserId)

    val retrieved = repository.getByRecallId(recallId)

    assertThat(retrieved, equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))
  }

  @Test
  @Transactional
  fun `can update an existing recall`() {
    repository.save(recall, currentUserId)

    assertThat(repository.getByRecallId(recallId), equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))

    val recallToUpdate = fullyPopulatedRecall(recallId)
    val nextCurrentUserId = ::UserId.random()
    createUserDetails(nextCurrentUserId)
    repository.save(recallToUpdate, nextCurrentUserId)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall, equalTo(recallToUpdate.copy(lastUpdatedByUserId = nextCurrentUserId.value)))
  }

  @Test
  fun `get by recallId throws RecallNotFoundException if a recall does not exist`() {
    assertThrows<RecallNotFoundException> {
      repository.getByRecallId(::RecallId.random())
    }
  }

  @Test
  @Transactional
  fun `can find an existing recall by recallId`() {
    repository.save(recall, currentUserId)

    val retrieved = repository.findByRecallId(recallId)

    assertThat(retrieved, equalTo(recall.copy(lastUpdatedByUserId = currentUserId.value)))
  }

  @Test
  fun `find by recallId returns null if a recall does not exist`() {
    assertThat(repository.findByRecallId(::RecallId.random()), absent())
  }

  @Test
  @Transactional
  fun `can find existing recalls by nomsNumber alone`() {
    repository.save(recall, currentUserId)

    val retrieved = repository.findByNomsNumber(nomsNumber)

    assertThat(retrieved, equalTo(listOf(recall.copy(lastUpdatedByUserId = currentUserId.value))))
  }

  @Test
  @Transactional
  fun `search by nomsNumber returns empty list given no matching recalls`() {
    repository.save(recall, currentUserId)

    assertThat(repository.findByNomsNumber(randomNoms()), equalTo(emptyList()))
  }

  @Test
  @Transactional
  fun `can find existing recalls by nomsNumber`() {
    repository.save(recall, currentUserId)

    val retrieved = repository.search(RecallSearchRequest(nomsNumber))

    assertThat(retrieved, equalTo(listOf(recall.copy(lastUpdatedByUserId = currentUserId.value))))
  }

  @Test
  @Transactional
  fun `search returns empty list given no matching recalls`() {
    repository.save(recall, currentUserId)

    assertThat(repository.search(RecallSearchRequest(randomNoms())), equalTo(emptyList()))
  }

  @Test
  @Transactional
  fun `can save a document by adding to recall`() {
    val documentId = ::DocumentId.random()
    val document = Document(
      documentId,
      recallId,
      PART_A_RECALL_REPORT,
      "PART_A.pdf",
      1,
      details,
      now,
      createdByUserId
    )
    val recallToUpdate = recall.copy(
      documents = setOf(
        document
      ),
    )
    repository.save(recallToUpdate, currentUserId)

    val persistedDocument = documentRepository.getByRecallIdAndDocumentId(recallId, documentId)
    assertThat(document, equalTo(persistedDocument))

    val createdRecall = repository.getByRecallId(recallId)

    assertThat(createdRecall, equalTo(recallToUpdate.copy(lastUpdatedByUserId = currentUserId.value)))
  }
}
