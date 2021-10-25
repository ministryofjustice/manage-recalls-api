package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional
import kotlin.jvm.Throws

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallRepositoryIntegrationTest(@Autowired private val repository: RecallRepository) {

  private val nomsNumber = randomNoms()
  private val recallId = ::RecallId.random()
  private val recall = Recall(recallId, nomsNumber)

  @Test
  @Transactional
  fun `saves and retrieves a recall`() {
    repository.save(recall)

    val retrieved = repository.getByRecallId(recallId)

    assertThat(retrieved, equalTo(Recall(recallId, nomsNumber)))
  }

  @Test
  @Transactional
  fun `can update an existing recall`() {
    repository.save(recall)

    assertThat(repository.getByRecallId(recallId), equalTo(recall))

    val recallToUpdate = fullyPopulatedRecall(recallId)
    repository.save(recallToUpdate)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall, equalTo(recallToUpdate))
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
    repository.save(recall)

    val retrieved = repository.findByRecallId(recallId)

    assertThat(retrieved, equalTo(Recall(recallId, nomsNumber)))
  }

  @Test
  fun `find by recallId returns null if a recall does not exist`() {
    assertThat(repository.findByRecallId(::RecallId.random()), absent())
  }

  @Test
  @Transactional
  fun `can find existing recalls by nomsNumber alone`() {
    repository.save(recall)

    val retrieved = repository.findByNomsNumber(nomsNumber)

    assertThat(retrieved, equalTo(listOf(Recall(recallId, nomsNumber))))
  }

  @Test
  @Transactional
  fun `search by nomsNumber returns empty list given no matching recalls`() {
    repository.save(recall)

    assertThat(repository.findByNomsNumber(randomNoms()), equalTo(emptyList()))
  }

  @Test
  @Transactional
  fun `can find existing recalls by nomsNumber`() {
    repository.save(recall)

    val retrieved = repository.search(RecallSearchRequest(nomsNumber))

    assertThat(retrieved, equalTo(listOf(Recall(recallId, nomsNumber))))
  }

  @Test
  @Transactional
  fun `search returns empty list given no matching recalls`() {
    repository.save(recall)

    assertThat(repository.search(RecallSearchRequest(randomNoms())), equalTo(emptyList()))
  }

  @Test
  @Transactional
  fun `can save a document without fileName`() {
    val recallToUpdate = Recall(
      recallId,
      nomsNumber,
      documents = setOf(
        VersionedDocument(
          UUID.randomUUID(),
          recallId.value,
          PART_A_RECALL_REPORT,
          "PART_A.pdf",
          OffsetDateTime.now()
        )
      ),
    )
    repository.save(recallToUpdate)

    val createdRecall = repository.getByRecallId(recallId)

    assertThat(createdRecall, equalTo(recallToUpdate))
  }

  @Test
  @Transactional
  fun `can add a document to an existing recall`() {
    val recallDocument = VersionedDocument(
      UUID.randomUUID(),
      recallId.value,
      PART_A_RECALL_REPORT,
      "PART_A.pdf",
      OffsetDateTime.now()
    )
    repository.save(recall)
    repository.addDocumentToRecall(recallId, recallDocument)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall.versionedDocuments, hasElement(recallDocument))
  }

  @Test
  @Transactional
  fun `addDocumentToRecall throws RecallNotFoundException if recall does not exist`() {
    val recallDocument = VersionedDocument(
      UUID.randomUUID(),
      recallId.value,
      PART_A_RECALL_REPORT,
      "PART_A.pdf",
      OffsetDateTime.now()
    )

    assertThrows<RecallNotFoundException> { repository.addDocumentToRecall(recallId, recallDocument) }
  }

  @Test
  @Transactional
  fun `can add a document to a recall with a document of the same category`() {
    val documentId = UUID.randomUUID()
    val existingDocument = VersionedDocument(
      documentId,
      recallId.value,
      PART_A_RECALL_REPORT,
      "originalFilename",
      OffsetDateTime.now()
    )
    val newDocument = VersionedDocument(
      documentId,
      recallId.value,
      PART_A_RECALL_REPORT,
      "newFilename",
      OffsetDateTime.now()
    )
    val existingRecall = Recall(recallId, nomsNumber, documents = setOf(existingDocument))

    repository.save(existingRecall)
    repository.addDocumentToRecall(recallId, newDocument)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall.versionedDocuments, hasElement(newDocument))
  }

  @Test
  @Transactional
  fun `can assign a recall`() {
    val assignee = ::UserId.random()
    val expected = Recall(recallId, nomsNumber, assignee = assignee)

    repository.save(Recall(recallId, nomsNumber))

    val assignedRecall = repository.assignRecall(recallId, assignee)
    assertThat(assignedRecall, equalTo(expected))
  }

  @Test
  @Transactional
  fun `can unassign a recall`() {
    val assignee = ::UserId.random()
    val expected = Recall(recallId, nomsNumber)

    repository.save(Recall(recallId, nomsNumber, assignee = assignee))

    val assignedRecall = repository.unassignRecall(recallId, assignee)
    assertThat(assignedRecall, equalTo(expected))
  }

  @Test
  @Transactional
  @Throws(NotFoundException::class)
  fun `can't unassign a recall when assignee doesnt match`() {
    val assignee = ::UserId.random()
    val otherAssignee = ::UserId.random()

    repository.save(Recall(recallId, nomsNumber, assignee = assignee))

    assertThrows<NotFoundException> { repository.unassignRecall(recallId, otherAssignee) }
  }
}
