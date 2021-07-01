package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
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

  @Test
  fun `saves and retrieves a recall`() {
    val randomUUID = UUID.randomUUID()
    val nomsNumber = "Random_name"
    val recall = Recall(randomUUID, nomsNumber)
    repository.save(recall)

    assertEquals(nomsNumber, repository.getById(randomUUID).nomsNumber)
  }
}
