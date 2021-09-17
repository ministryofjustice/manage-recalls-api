package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.UUID
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class RecallDocumentRepositoryIntegrationTest(
  @Autowired private val documentRepository: RecallDocumentRepository,
  @Autowired private val recallRepository: RecallRepository
) {

  private val recallId = ::RecallId.random()
  private val recallDocument = RecallDocument(UUID.randomUUID(), recallId.value, RECALL_NOTIFICATION, "file_name")

  @Test
  @Transactional
  fun `upload a recall document for a recall`() {
    recallRepository.save(Recall(recallId, NomsNumber("AB1234C")))
    documentRepository.save(recallDocument)

    val retrieved = documentRepository.findByRecallIdAndCategory(recallId.value, RECALL_NOTIFICATION)

    assertThat(retrieved, equalTo(recallDocument))
  }
}
