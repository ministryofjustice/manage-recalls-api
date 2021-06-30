package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@DataJpaTest
@AutoConfigureTestDatabase()
class RecallRepositoryIntegrationTest {

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var repository: RecallRepository

  @BeforeEach
  internal fun setUp() {
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.clean()
    flyway.migrate()
  }

  @Test
  fun saveRecall() {
    val randomUUID = UUID.randomUUID()
    val recall = Recall(randomUUID, "Random_name")
    repository.save(recall)

    assertEquals("Random_name", repository.getById(randomUUID).userId)
  }
}