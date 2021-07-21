package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
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
  fun `updates a recall with a revocation order doc s3 key`() {
    val recall = Recall(recallId, nomsNumber)
    repository.save(recall)

    val revocationOrderDocS3Key = UUID.randomUUID()
    val recallWithRevocationOrder = Recall(recallId, nomsNumber, revocationOrderDocS3Key)
    repository.save(recallWithRevocationOrder)

    val actualRecall = repository.getByRecallId(recallId)

    assertThat(actualRecall, equalTo(recallWithRevocationOrder))
  }

  @Test
  fun `updates a recall with a document`() {
    val originalRecall = Recall(recallId, nomsNumber)
    repository.save(originalRecall)

    assertThat(repository.getByRecallId(recallId), equalTo(originalRecall))

    val recallDocumentId = UUID.randomUUID()
    val document = RecallDocument(recallDocumentId, recallId.value, PART_A_RECALL_REPORT)

    val recallWithDocument = Recall(recallId, nomsNumber, null, setOf(document))
    repository.save(recallWithDocument)

    val actualRecall = repository.getByRecallId(recallId)

    assertThat(actualRecall, equalTo(recallWithDocument))
  }
}
