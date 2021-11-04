package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomUnVersionedDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomVersionedDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentNotFoundException
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class DocumentRepositoryIntegrationTest(
  @Autowired private val documentRepository: DocumentRepository,
  @Autowired private val recallRepository: RecallRepository
) {

  private val recallId = ::RecallId.random()
  private val nomsNumber = randomNoms()
  private val recall = Recall(recallId, nomsNumber, ::UserId.random(), OffsetDateTime.now())
  private val versioneDocumentId = ::DocumentId.random()
  // TODO: parameterized tests driven from RecallDocumentCategory
  private val versionedCategory = randomVersionedDocumentCategory()
  private val unVersionedCategory = randomUnVersionedDocumentCategory()
  private val versionedRecallDocument = versionedDocument(versioneDocumentId, recallId, versionedCategory, 1)

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush two distinct copies of an un-versioned document for an existing recall`() {
    recallRepository.save(recall)

    val idOne = ::DocumentId.random()
    val docOne = unVersionedDocument(idOne, recallId, unVersionedCategory)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = unVersionedDocument(idTwo, recallId, unVersionedCategory)
    documentRepository.saveAndFlush(docTwo)

    val retrievedOne = documentRepository.getByRecallIdAndDocumentId(recallId, idOne)
    val retrievedTwo = documentRepository.getByRecallIdAndDocumentId(recallId, idTwo)

    assertThat(retrievedOne, equalTo(docOne))
    assertThat(retrievedTwo, equalTo(docTwo))
  }

  @Test
  @Transactional
  fun `can save and flush two distinct copies of an versioned document with different versions for an existing recall`() {
    recallRepository.save(recall)

    val idOne = ::DocumentId.random()
    val docOne = versionedDocument(idOne, recallId, versionedCategory, 1)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = versionedDocument(idTwo, recallId, versionedCategory, 2)
    documentRepository.saveAndFlush(docTwo)

    val retrievedOne = documentRepository.getByRecallIdAndDocumentId(recallId, idOne)
    val retrievedTwo = documentRepository.getByRecallIdAndDocumentId(recallId, idTwo)

    assertThat(retrievedOne, equalTo(docOne))
    assertThat(retrievedTwo, equalTo(docTwo))
  }

  @Test
  @Transactional
  fun `cannot save and flush two distinct copies of an versioned document with the same version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall)

    val idOne = ::DocumentId.random()
    val docOne = versionedDocument(idOne, recallId, versionedCategory, 1)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = versionedDocument(idTwo, recallId, versionedCategory, 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(docTwo)
    }
    assertThat(thrown.message!!.substring(0, 27), equalTo("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a versioned document with null version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall)

    val id = ::DocumentId.random()
    val document = versionedDocument(id, recallId, versionedCategory, 1).copy(version = null)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(document)
    }
    assertThat(thrown.message!!.substring(0, 27), equalTo("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush an un-versioned document with non-null version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall)

    val id = ::DocumentId.random()
    val document = unVersionedDocument(id, recallId, unVersionedCategory).copy(version = 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(document)
    }
    assertThat(thrown.message!!.substring(0, 27), equalTo("could not execute statement"))
  }

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId for an existing recall`() {
    recallRepository.save(recall)
    documentRepository.save(versionedRecallDocument)

    val retrieved = documentRepository.getByRecallIdAndDocumentId(recallId, versioneDocumentId)

    assertThat(retrieved, equalTo(versionedRecallDocument))
  }

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId throws RecallDocumentNotFoundException if the document does not exist`() {
    val thrown = assertThrows<DocumentNotFoundException> {
      documentRepository.getByRecallIdAndDocumentId(
        recallId,
        versioneDocumentId
      )
    }

    assertThat(thrown, equalTo(DocumentNotFoundException(recallId, versioneDocumentId)))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory for an existing recall`() {
    recallRepository.save(recall)
    documentRepository.save(versionedRecallDocument)

    val retrieved = documentRepository.findByRecallIdAndCategory(recallId.value, versionedCategory)

    assertThat(retrieved, equalTo(versionedRecallDocument))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory returns null if no document exists`() {
    recallRepository.save(recall)

    val retrieved = documentRepository.findByRecallIdAndCategory(recallId.value, versionedCategory)

    assertThat(retrieved, absent())
  }

  private fun versionedDocument(id: DocumentId, recallId: RecallId, category: RecallDocumentCategory, version: Int) =
    testDocument(id, recallId, category, version)

  private fun unVersionedDocument(id: DocumentId, recallId: RecallId, category: RecallDocumentCategory) =
    testDocument(id, recallId, category, null)

  private fun testDocument(id: DocumentId, recallId: RecallId, category: RecallDocumentCategory, version: Int?): Document {
    return Document(
      id,
      recallId,
      category,
      "file_name",
      version,
      OffsetDateTime.now()
    )
  }
}
