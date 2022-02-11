package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RescindRecordRepositoryIntegrationTest(
  @Autowired private val rescindRecordRepository: RescindRecordRepository,
  @Autowired private val documentRepository: DocumentRepository,
) : IntegrationTestBase() {

  private val documentId = ::DocumentId.random()

  val requestCategory = DocumentCategory.RESCIND_REQUEST_EMAIL

  @Test
  @Transactional
  fun `cannot save and flush 2 records for the same recall and version`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.saveAndFlush(
      Document(
        documentId,
        recallId,
        requestCategory,
        "blah",
        null,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    )

    val idOne = ::RescindRecordId.random()
    val recordOne = RescindRecord(idOne, recallId, 1, createdByUserId, OffsetDateTime.now(), documentId, "Blah", LocalDate.now())
    rescindRecordRepository.save(recordOne)
    val idTwo = ::RescindRecordId.random()
    val recordTwo = RescindRecord(idTwo, recallId, 1, createdByUserId, OffsetDateTime.now(), documentId, "Blah", LocalDate.now())

    val thrown = assertThrows<DataIntegrityViolationException> {
      rescindRecordRepository.saveAndFlush(recordTwo)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save a record which only has some of the decision fields populated for the same recall and version`() {
    recallRepository.save(recall, currentUserId)
    documentRepository.saveAndFlush(
      Document(
        documentId,
        recallId,
        requestCategory,
        "blah",
        null,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    )

    val idOne = ::RescindRecordId.random()
    val record = RescindRecord(idOne, recallId, 1, createdByUserId, OffsetDateTime.now(), documentId, "Blah", LocalDate.now(), true)

    val thrown = assertThrows<DataIntegrityViolationException> {
      rescindRecordRepository.saveAndFlush(record)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }
}
