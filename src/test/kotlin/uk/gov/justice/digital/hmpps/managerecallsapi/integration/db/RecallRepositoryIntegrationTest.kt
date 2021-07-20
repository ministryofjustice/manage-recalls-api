package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
class RecallRepositoryIntegrationTest(
  @Autowired
  private val repository: RecallRepository
) {

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A12345F")

  @Test
  fun `saves and retrieves a recall`() {
    val recall = Recall(recallId, nomsNumber, null, emptySet())
    repository.save(recall)

    val actualRecall = repository.getById(recallId.value)

    assertThat(actualRecall, equalTo(recall))
  }

  @Test
  fun `updates a recall with a revocation order doc s3 key`() {
    val recall = Recall(recallId, nomsNumber, null, emptySet())
    repository.save(recall)

    val revocationOrderDocS3Key = UUID.randomUUID()
    val recallWithRevocationOrder = Recall(recallId, nomsNumber, revocationOrderDocS3Key, emptySet())
    repository.save(recallWithRevocationOrder)

    val actualRecall = repository.getById(recallId.value)

    assertThat(actualRecall, equalTo(recallWithRevocationOrder))
  }

  @Test
  fun `updates a recall with a document`() {
    val recall = Recall(recallId, nomsNumber, null, emptySet())
    repository.save(recall)

    val recallDocumentId = UUID.randomUUID()
    val document = RecallDocument(recallDocumentId, recallId.value, PART_A_RECALL_REPORT)

    repository.save(
      Recall(
        recallId,
        nomsNumber,
        null,
        setOf(document)
      )
    )

    val actualRecall = repository.getById(recallId.value)

    assertThat(actualRecall.documents, hasSize(equalTo(1)))
    assertThat(actualRecall.documents.first(), equalTo(document))
  }
}
