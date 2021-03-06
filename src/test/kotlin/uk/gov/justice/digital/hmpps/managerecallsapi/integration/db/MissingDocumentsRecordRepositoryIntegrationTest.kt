package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomVersionedDocumentCategory
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class MissingDocumentsRecordRepositoryIntegrationTest(
  @Autowired private val missingDocumentsRecordRepository: MissingDocumentsRecordRepository,
  @Autowired private val documentRepository: DocumentRepository,
) : IntegrationTestBase() {

  private val documentId = ::DocumentId.random()
  // TODO: parameterized tests driven from RecallDocumentCategory
  private val versionedCategory = randomVersionedDocumentCategory()

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush two distinct copies of an missing documents record for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.saveAndFlush(
      Document(
        documentId,
        recallId,
        versionedCategory,
        FileName("blah"),
        1,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    )

    val idOne = ::MissingDocumentsRecordId.random()
    val mdrOne = MissingDocumentsRecord(idOne, recallId, setOf(versionedCategory), documentId, "Blah", 1, createdByUserId, OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrOne)
    val idTwo = ::MissingDocumentsRecordId.random()
    val mdrTwo = MissingDocumentsRecord(idTwo, recallId, setOf(versionedCategory), documentId, "Blah", 2, createdByUserId, OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrTwo)

    val retrievedOne = missingDocumentsRecordRepository.getById(idOne.value)
    val retrievedTwo = missingDocumentsRecordRepository.getById(idTwo.value)

    assertThat(retrievedOne, equalTo(mdrOne))
    assertThat(retrievedTwo, equalTo(mdrTwo))
  }

  @Test
  @Transactional
  fun `cannot save and flush 2 MDR for the same recall and version`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.saveAndFlush(
      Document(
        documentId,
        recallId,
        versionedCategory,
        FileName("blah"),
        1,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    )

    val idOne = ::MissingDocumentsRecordId.random()
    val mdrOne = MissingDocumentsRecord(idOne, recallId, setOf(versionedCategory), documentId, "Blah", 1, createdByUserId, OffsetDateTime.now())
    missingDocumentsRecordRepository.saveAndFlush(mdrOne)
    val idTwo = ::MissingDocumentsRecordId.random()
    val mdrTwo = MissingDocumentsRecord(idTwo, recallId, setOf(versionedCategory), documentId, "Blah", 1, createdByUserId, OffsetDateTime.now())

    val thrown = assertThrows<DataIntegrityViolationException> {
      missingDocumentsRecordRepository.saveAndFlush(mdrTwo)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }
}
