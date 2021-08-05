package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.util.UUID

@ExtendWith(SpringExtension::class)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@DataJpaTest
@AutoConfigureTestDatabase
@Import(RecallRepository::class)
class RecallRepositoryIntegrationTest(
  @Autowired
  private val repository: RecallRepository
) {
  // TODO: Avoid bugs such as PUD-329 by use of a real database for integration tests such as this, i.e. PUD-330.

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A12345F")

  @Test
  fun `saves and retrieves a recall`() {
    val recall = Recall(recallId, nomsNumber)
    repository.save(recall)

    val retrieved = repository.getByRecallId(recallId)

    assertThat(retrieved, equalTo(Recall(recallId, nomsNumber)))
  }

  @Test
  fun `can update an existing recall`() {
    val originalRecall = Recall(recallId, nomsNumber)
    repository.save(originalRecall)

    assertThat(repository.getByRecallId(recallId), equalTo(originalRecall))

    val recallToUpdate = originalRecall.copy(
      revocationOrderDocS3Key = UUID.randomUUID(),
      documents = setOf(RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT)),
      recallType = FIXED,
      agreeWithRecallRecommendation = true,
      recallLength = TWENTY_EIGHT_DAYS
    )
    repository.save(recallToUpdate)

    val updatedRecall = repository.getByRecallId(recallId)

    assertThat(updatedRecall, equalTo(recallToUpdate))
  }

  @Test
  fun `get by recallId throws RecallNotFoundException if a recall does not exist`() {
    assertThrows<RecallNotFoundException> {
      repository.getByRecallId(recallId)
    }
  }
}
