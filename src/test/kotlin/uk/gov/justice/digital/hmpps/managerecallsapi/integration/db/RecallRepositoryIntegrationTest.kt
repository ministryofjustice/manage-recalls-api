package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class RecallRepositoryIntegrationTest {

  @Autowired
  private lateinit var dataSource: DataSource

  @BeforeEach
  internal fun setUp() {
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.clean()
    flyway.migrate()
  }

  @Test
  fun name() {
    val prepareStatement = dataSource.connection.prepareStatement("SELECT 1")
    println("****" + prepareStatement.execute())
  }

}