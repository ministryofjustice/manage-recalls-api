package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentNotFoundException
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class VersionedDocumentRepositoryIntegrationTest(
  @Autowired private val documentRepository: VersionedDocumentRepository,
  @Autowired private val recallRepository: RecallRepository
) {

  private val recallId = ::RecallId.random()
  private val documentId = ::DocumentId.random()
  private val category = RECALL_NOTIFICATION
  private val recallDocument = VersionedDocument(
    documentId,
    recallId,
    category,
    "file_name",
    OffsetDateTime.now()
  )

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId for an existing recall`() {
    recallRepository.save(Recall(recallId, NomsNumber("AB1234C"), OffsetDateTime.now()))
    documentRepository.save(recallDocument)

    val retrieved = documentRepository.getByRecallIdAndDocumentId(recallId, documentId)

    assertThat(retrieved, equalTo(recallDocument))
  }

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId throws RecallDocumentNotFoundException if the document does not exist`() {
    val thrown = assertThrows<RecallDocumentNotFoundException> {
      documentRepository.getByRecallIdAndDocumentId(
        recallId,
        documentId
      )
    }

    assertThat(thrown, equalTo(RecallDocumentNotFoundException(recallId, documentId)))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory for an existing recall`() {
    recallRepository.save(Recall(recallId, NomsNumber("AB1234C"), OffsetDateTime.now()))
    documentRepository.save(recallDocument)

    val retrieved = documentRepository.findByRecallIdAndCategory(recallId.value, category)

    assertThat(retrieved, equalTo(recallDocument))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory returns null if no document exists`() {
    recallRepository.save(Recall(recallId, NomsNumber("AB1234C"), OffsetDateTime.now()))

    val retrieved = documentRepository.findByRecallIdAndCategory(recallId.value, category)

    assertThat(retrieved, absent())
  }
}
