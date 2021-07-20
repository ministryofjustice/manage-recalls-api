package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isEmpty
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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import java.util.UUID

@ExtendWith(SpringExtension::class)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@DataJpaTest
@AutoConfigureTestDatabase
class RecallRepositoryIntegrationTest(
  @Autowired
  private val repository: RecallRepository
) {

  val nomsNumber = "A12345F"

  @Test
  fun `saves and retrieves a recall`() {
    val recallId = UUID.randomUUID()
    repository.save(
      Recall(
        id = recallId,
        nomsNumber = nomsNumber
      )
    )

    val actualRecall = repository.getById(recallId)

    assertThat(nomsNumber, equalTo(actualRecall.nomsNumber))
    assertThat(actualRecall.revocationOrderDocS3Key, equalTo(null))
    assertThat(actualRecall.documents, isEmpty)
  }

  @Test
  fun `updates a recall with a revocation order doc s3 key`() {
    val recallId = UUID.randomUUID()
    repository.save(
      Recall(
        id = recallId,
        nomsNumber = nomsNumber
      )
    )

    val revocationOrderDocS3Key = UUID.randomUUID()
    repository.save(
      Recall(
        id = recallId,
        nomsNumber = nomsNumber,
        revocationOrderDocS3Key = revocationOrderDocS3Key
      )
    )

    val actualRecall = repository.getById(recallId)

    assertThat(actualRecall.nomsNumber, equalTo(nomsNumber))
    assertThat(actualRecall.revocationOrderDocS3Key, equalTo(revocationOrderDocS3Key))
  }

  @Test
  fun `updates a recall with a document`() {
    val recallId = UUID.randomUUID()
    repository.save(
      Recall(
        id = recallId,
        nomsNumber = nomsNumber
      )
    )

    val document = RecallDocument(
      id = UUID.randomUUID(),
      recallId = recallId,
      category = RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    repository.save(
      Recall(
        id = recallId,
        nomsNumber = nomsNumber,
        documents = setOf(
          document
        )
      )
    )

    val actualRecall = repository.getById(recallId)

    assertThat(actualRecall.documents, hasSize(equalTo(1)))
    assertThat(actualRecall.documents.first(), equalTo(document))
  }
}
