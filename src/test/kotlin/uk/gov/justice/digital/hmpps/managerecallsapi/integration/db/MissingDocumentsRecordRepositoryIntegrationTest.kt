package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomVersionedDocumentCategory
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class MissingDocumentsRecordRepositoryIntegrationTest(
  @Autowired private val missingDocumentsRecordRepository: MissingDocumentsRecordRepository,
  @Autowired private val documentRepository: DocumentRepository,
  @Autowired private val recallRepository: RecallRepository
) {

  private val recallId = ::RecallId.random()
  private val nomsNumber = randomNoms()
  private val recall = Recall(recallId, nomsNumber, ::UserId.random(), OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"))
  private val documentId = ::DocumentId.random()
  // TODO: parameterized tests driven from RecallDocumentCategory
  private val versionedCategory = randomVersionedDocumentCategory()

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush two distinct copies of an missing documents record for an existing recall`() {
    recallRepository.save(recall)
    documentRepository.saveAndFlush(Document(documentId, recallId, versionedCategory, "blah", 1, OffsetDateTime.now()))

    val idOne = ::MissingDocumentsRecordId.random()
    val mdrOne = MissingDocumentsRecord(idOne, recallId, setOf(versionedCategory), documentId, "Blah", 1, ::UserId.random(), OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrOne)
    val idTwo = ::MissingDocumentsRecordId.random()
    val mdrTwo = MissingDocumentsRecord(idTwo, recallId, setOf(versionedCategory), documentId, "Blah", 2, ::UserId.random(), OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrTwo)

    val retrievedOne = missingDocumentsRecordRepository.getById(idOne.value)
    val retrievedTwo = missingDocumentsRecordRepository.getById(idTwo.value)

    assertThat(retrievedOne, equalTo(mdrOne))
    assertThat(retrievedTwo, equalTo(mdrTwo))
  }

  @Test
  @Transactional
  fun `cannot save and flush 2 MDR for the same recall and version`() {
    recallRepository.save(recall)
    documentRepository.saveAndFlush(Document(documentId, recallId, versionedCategory, "blah", 1, OffsetDateTime.now()))

    val idOne = ::MissingDocumentsRecordId.random()
    val mdrOne = MissingDocumentsRecord(idOne, recallId, setOf(versionedCategory), documentId, "Blah", 1, ::UserId.random(), OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrOne)
    val idTwo = ::MissingDocumentsRecordId.random()
    val mdrTwo = MissingDocumentsRecord(idTwo, recallId, setOf(versionedCategory), documentId, "Blah", 1, ::UserId.random(), OffsetDateTime.now())

    val thrown = assertThrows<DataIntegrityViolationException> {
      missingDocumentsRecordRepository.saveAndFlush(mdrTwo)
    }
    assertThat(thrown.message!!.substring(0, 27), equalTo("could not execute statement"))
  }
}
