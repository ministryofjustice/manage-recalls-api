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
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallRepositoryIntegrationTest(@Autowired private val repository: RecallRepository) {

  private val nomsNumber = NomsNumber("A12345F")
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

    val localDate = LocalDate.now()
    val recallToUpdate = recall.copy(
      recallType = FIXED,
      revocationOrderId = UUID.randomUUID(),
      documents = setOf(RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT, randomString())),
      recallLength = TWENTY_EIGHT_DAYS,
      recallEmailReceivedDateTime = OffsetDateTime.now(),
      lastReleasePrison = "WIN",
      lastReleaseDate = localDate,
      localPoliceForce = "London",
      contrabandDetail = "i am worried...",
      vulnerabilityDiversityDetail = "has the following needs",
      mappaLevel = MappaLevel.NOT_KNOWN,
      sentencingInfo = SentencingInfo(
        localDate,
        localDate,
        localDate,
        "A Court",
        "Some Offence",
        SentenceLength(2, 4, 6),
        localDate
      ),
      bookingNumber = "BN12345",
      probationInfo = ProbationInfo(
        "Probation Officer Name",
        "07111111111",
        "email@email.com",
        ProbationDivision.NORTH_EAST,
        "Assistant Chief Officer"
      ),
      licenceConditionsBreached = "Breached by blah blah blah",
      reasonsForRecall = setOf(ReasonForRecall.ELM_FURTHER_OFFENCE),
      reasonsForRecallOtherDetail = "Because of something else",
      currentPrison = "MWI",
      additionalLicenceConditions = true,
      additionalLicenceConditionsDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      differentNomsNumber = false,
      differentNomsNumberDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
    )
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
  fun `can save a document without fileName`() {
    val recallToUpdate = Recall(
      recallId,
      nomsNumber,
      documents = setOf(RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT, null)),
    )
    repository.save(recallToUpdate)

    val createdRecall = repository.getByRecallId(recallId)

    assertThat(createdRecall, equalTo(recallToUpdate))
  }

  @Test
  @Transactional
  fun `can add a document to an existing recall`() {
    val recallDocument = RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT, null)
    repository.save(recall)
    repository.addDocumentToRecall(recallId, recallDocument)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall.documents, hasElement(recallDocument))
  }

  @Test
  @Transactional
  fun `can add a document to a recall with a document of the same category`() {
    val documentId = UUID.randomUUID()
    val existingDocument = RecallDocument(documentId, recallId.value, PART_A_RECALL_REPORT, "originalFilename")
    val newDocument = RecallDocument(documentId, recallId.value, PART_A_RECALL_REPORT, "newFilename")
    val existingRecall = Recall(recallId, nomsNumber, documents = setOf(existingDocument))

    repository.save(existingRecall)
    repository.addDocumentToRecall(recallId, newDocument)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall.documents, hasElement(newDocument))
  }
}
