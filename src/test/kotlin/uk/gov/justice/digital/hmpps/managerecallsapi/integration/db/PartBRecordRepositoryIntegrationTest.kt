package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomFileName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.transaction.Transactional

class PartBRecordRepositoryIntegrationTest(
  @Autowired private val partBRecordRepository: PartBRecordRepository,
  @Autowired private val documentRepository: DocumentRepository,
) : IntegrationTestBase() {

  private val fileName = randomFileName()
  private val details = randomString()
  private val partBReceivedDate = LocalDate.now()

  // PartBRecord: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush one then a second partBRecord with valid version values when all referenced documents already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    val partBRecordId1 = ::PartBRecordId.random()
    val partBRecord1 = partBRecord(partBRecordId1, 1, partBDocumentId = savePartB(), emailId = saveEmail())
      .copy(oasysDocumentId = saveOasys().value)

    partBRecordRepository.saveAndFlush(partBRecord1)
    val retrieved1 = partBRecordRepository.getById(partBRecordId1.value)

    assertThat(retrieved1, equalTo(partBRecord1))

    val partBRecordId2 = ::PartBRecordId.random()
    val partBRecord2 = partBRecord1
      .copy(id = partBRecordId2.value, version = 2)

    partBRecordRepository.saveAndFlush(partBRecord2)
    val retrieved2 = partBRecordRepository.getById(partBRecordId2.value)

    assertThat(retrieved2, equalTo(partBRecord2))
  }

  @Test
  @Transactional
  fun `can save and flush a partBRecord without oasys when record part B and email documents already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    val partBRecordId = ::PartBRecordId.random()
    val partBRecord = partBRecord(partBRecordId, 1, partBDocumentId = savePartB(), emailId = saveEmail())
      .copy(oasysDocumentId = null)

    partBRecordRepository.saveAndFlush(partBRecord)
    val retrieved1 = partBRecordRepository.getById(partBRecordId.value)

    assertThat(retrieved1, equalTo(partBRecord))
  }

  @Test
  @Transactional
  fun `cannot save and flush a partBRecord when partB document NOT already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecordId = ::PartBRecordId.random()
    val partBRecord = partBRecord(partBRecordId, 1, partBDocumentId = ::DocumentId.random(), emailId = saveEmail())
      .copy(oasysDocumentId = saveOasys().value)

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a partBRecord when email document NOT already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecordId = ::PartBRecordId.random()
    val partBRecord = partBRecord(partBRecordId, 1, partBDocumentId = savePartB(), emailId = ::DocumentId.random())
      .copy(oasysDocumentId = saveOasys().value)

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a partBRecord for a non-existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecordId = ::PartBRecordId.random()
    val partBRecord = partBRecord(partBRecordId, 1, partBDocumentId = savePartB(), emailId = saveEmail())
      .copy(recallId = ::RecallId.random().value)

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush two partBRecords with the same version value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecordId1 = ::PartBRecordId.random()
    val partBRecord1 = partBRecord(partBRecordId1, 1, partBDocumentId = savePartB(), emailId = saveEmail())

    partBRecordRepository.saveAndFlush(partBRecord1)
    val retrieved1 = partBRecordRepository.getById(partBRecordId1.value)

    assertThat(retrieved1, equalTo(partBRecord1))

    val partBRecord2 = partBRecord1.copy(id = ::PartBRecordId.random().value)

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord2)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a partBRecord with an invalid(0) version value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecord = partBRecord(::PartBRecordId.random(), 0, partBDocumentId = savePartB(), emailId = saveEmail())

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a partBRecord with an invalid(negative) version value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val partBRecord = partBRecord(::PartBRecordId.random(), -1, partBDocumentId = savePartB(), emailId = saveEmail())

    val thrown = assertThrows<DataIntegrityViolationException> {
      partBRecordRepository.saveAndFlush(partBRecord)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  private fun partBRecord(
    id: PartBRecordId,
    version: Int,
    partBDocumentId: DocumentId,
    emailId: DocumentId,
  ) =
    PartBRecord(
      id,
      recallId,
      details,
      partBReceivedDate,
      partBDocumentId,
      emailId,
      null,
      version,
      createdByUserId,
      OffsetDateTime.now()
    )

  private fun savePartB(docId: DocumentId = ::DocumentId.random()) =
    saveDocument(docId, DocumentCategory.PART_B_RISK_REPORT, 1)

  private fun saveEmail(docId: DocumentId = ::DocumentId.random()) =
    saveDocument(docId, DocumentCategory.PART_B_EMAIL_FROM_PROBATION, null)

  private fun saveOasys(docId: DocumentId = ::DocumentId.random()) =
    saveDocument(docId, DocumentCategory.OASYS_RISK_ASSESSMENT, 1)

  private fun saveDocument(
    docId: DocumentId = ::DocumentId.random(),
    category: DocumentCategory,
    version: Int?
  ) =
    documentRepository.saveAndFlush(
      Document(
        docId,
        recallId,
        category,
        fileName,
        version,
        details,
        OffsetDateTime.now(),
        createdByUserId
      )
    ).id.let { DocumentId(it) }
}
