package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall
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
class RecallRepositoryIntegrationTest(
  @Autowired
  private val repository: RecallRepository
) {
  private val nomsNumber = NomsNumber("A12345F")

  @Test
  @Transactional
  fun `saves and retrieves a recall`() {
    val recallId = ::RecallId.random()
    val recall = Recall(recallId, nomsNumber)
    repository.save(recall)

    val retrieved = repository.getByRecallId(recallId)

    assertThat(retrieved, equalTo(Recall(recallId, nomsNumber)))
  }

  @Test
  @Transactional
  fun `can update an existing recall`() {
    val recallId = ::RecallId.random()
    val originalRecall = Recall(recallId, nomsNumber)
    repository.save(originalRecall)

    assertThat(repository.getByRecallId(recallId), equalTo(originalRecall))

    val localDate = LocalDate.now()
    val recallToUpdate = originalRecall.copy(
      recallType = FIXED,
      revocationOrderId = UUID.randomUUID(),
      documents = setOf(RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT)),
      recallLength = TWENTY_EIGHT_DAYS,
      agreeWithRecallRecommendation = true,
      recallEmailReceivedDateTime = OffsetDateTime.now(),
      lastReleasePrison = "A Prison",
      lastReleaseDate = localDate,
      localPoliceForce = "London",
      contrabandDetail = "i am worried...",
      vulnerabilityDiversityDetail = "has the following needs",
      mappaLevel = MappaLevel.NOT_KNOWN,
      sentencingInfo = SentencingInfo(localDate, localDate, localDate, "A Court", "Some Offence", SentenceLength(2, 4, 6), localDate),
      bookingNumber = "BN12345",
      probationInfo = ProbationInfo("Probation Officer Name", "07111111111", "email@email.com", ProbationDivision.NORTH_EAST, "Assistant Chief Officer"),
      licenceConditionsBreached = "Breached by blah blah blah",
      reasonsForRecall = setOf(ReasonForRecall.ELM_FURTHER_OFFENCE)
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
}
