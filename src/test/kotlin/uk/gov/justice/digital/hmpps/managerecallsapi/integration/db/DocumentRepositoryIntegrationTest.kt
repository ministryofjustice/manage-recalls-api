package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
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
) : IntegrationTestBase() {

  private val documentId = ::DocumentId.random()
  // TODO: parameterized tests driven from RecallDocumentCategory
  private val versionedCategory = randomVersionedDocumentCategory()
  private val unVersionedCategory = randomUnVersionedDocumentCategory()
  private val versionedRecallDocument = versionedDocument(documentId, recallId, versionedCategory, 1)

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush two distinct copies of an un-versioned document for an existing recall`() {
    recallRepository.save(recall, currentUserId)

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
  fun `can save and flush two distinct copies of a versioned document with different valid versions for an existing recall`() {
    recallRepository.save(recall, currentUserId)

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
  fun `cannot save and flush version 0 of a versioned document throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val documentWithBlankDetails = versionedDocument(id, recallId, versionedCategory, 0)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(documentWithBlankDetails)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush version -1 of a versioned document throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val documentWithBlankDetails = versionedDocument(id, recallId, versionedCategory, -1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(documentWithBlankDetails)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `when storing two distinct copies of a versioned document with different versions for an existing recall, getLatest returns the latest`() {
    recallRepository.save(recall, currentUserId)

    val idOne = ::DocumentId.random()
    val docOne = versionedDocument(idOne, recallId, versionedCategory, 1)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = versionedDocument(idTwo, recallId, versionedCategory, 2)
    documentRepository.saveAndFlush(docTwo)

    val latest = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(latest, equalTo(docTwo))
  }

  @Test
  @Transactional
  fun `when storing two distinct copies of a versioned document with different versions for an existing recall, then deleting the latest, getLatest returns the first`() {
    recallRepository.save(recall, currentUserId)

    val idOne = ::DocumentId.random()
    val docOne = versionedDocument(idOne, recallId, versionedCategory, 1)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = versionedDocument(idTwo, recallId, versionedCategory, 2)
    documentRepository.saveAndFlush(docTwo)
    documentRepository.deleteByDocumentId(idTwo)

    val latest = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(latest, equalTo(docOne))
  }

  @Test
  @Transactional
  fun `findLatestVersionedDocumentByRecallIdAndCategory throws exception when trying to find an unversioned category`() {
    assertThrows<WrongDocumentTypeException> {
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, unVersionedCategory)
    }
  }

  @Test
  @Transactional
  fun `cannot save and flush two distinct copies of an versioned document with the same version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val idOne = ::DocumentId.random()
    val docOne = versionedDocument(idOne, recallId, versionedCategory, 1)
    documentRepository.saveAndFlush(docOne)
    val idTwo = ::DocumentId.random()
    val docTwo = versionedDocument(idTwo, recallId, versionedCategory, 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(docTwo)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a versioned document with null version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val document = versionedDocument(id, recallId, versionedCategory, 1).copy(version = null)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(document)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `can save and flush a versioned document with details for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val details = "Random document details"
    val id = ::DocumentId.random()
    val documentWithDetails = versionedDocument(id, recallId, versionedCategory, 1, details)

    documentRepository.saveAndFlush(documentWithDetails)

    val latest = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(latest, equalTo(documentWithDetails))
  }

  @Test
  @Transactional
  fun `can save and flush version 1 of a versioned document with blank details for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val documentWithBlankDetails = versionedDocument(id, recallId, versionedCategory, 1, " ")

    documentRepository.saveAndFlush(documentWithBlankDetails)

    val latest = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(latest, equalTo(documentWithBlankDetails))
  }

  @Test
  @Transactional
  fun `cannot save and flush version 2 of a versioned document with blank details for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val documentWithBlankDetails = versionedDocument(id, recallId, versionedCategory, 2, " ")

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(documentWithBlankDetails)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush an un-versioned document with non-null version for an existing recall throws DataIntegrityViolationException`() {
    recallRepository.save(recall, currentUserId)

    val id = ::DocumentId.random()
    val document = unVersionedDocument(id, recallId, unVersionedCategory).copy(version = 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      documentRepository.saveAndFlush(document)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.save(versionedRecallDocument)

    val retrieved = documentRepository.getByRecallIdAndDocumentId(recallId, documentId)

    assertThat(retrieved, equalTo(versionedRecallDocument))
  }

  @Test
  @Transactional
  fun `getByRecallIdAndDocumentId throws RecallDocumentNotFoundException if the document does not exist`() {
    val thrown = assertThrows<DocumentNotFoundException> {
      documentRepository.getByRecallIdAndDocumentId(
        recallId,
        documentId
      )
    }

    assertThat(thrown, equalTo(DocumentNotFoundException(recallId, documentId)))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.save(versionedRecallDocument)

    val retrieved = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(retrieved, equalTo(versionedRecallDocument))
  }

  @Test
  @Transactional
  fun `findByRecallIdAndCategory returns null if no document exists`() {
    recallRepository.save(recall, currentUserId)

    val retrieved = documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, versionedCategory)

    assertThat(retrieved, absent())
  }

  private fun versionedDocument(id: DocumentId, recallId: RecallId, category: DocumentCategory, version: Int, details: String? = "details") =
    testDocument(id, recallId, category, version, details)

  private fun unVersionedDocument(id: DocumentId, recallId: RecallId, category: DocumentCategory, details: String? = "details") =
    testDocument(id, recallId, category, null, details)

  private fun testDocument(id: DocumentId, recallId: RecallId, category: DocumentCategory, version: Int?, details: String?): Document {
    return Document(
      id,
      recallId,
      category,
      "file_name",
      version,
      details,
      OffsetDateTime.now(),
      createdByUserId
    )
  }
}
