package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.flywaydb.core.Flyway
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallResponse
import javax.sql.DataSource

@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@AutoConfigureTestDatabase
class BookARecallIntegrationTest : IntegrationTestBase() {

  private val nomsNumber = "123456"
  private val bookRecallRequest = BookRecallRequest(nomsNumber)

  @Autowired private lateinit var dataSource: DataSource

  @BeforeEach
  internal fun setUp() {
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.clean()
    flyway.migrate()
  }

  @Test
  fun `should respond with 401 if user does not have the MANAGE_RECALLS role`() {
    val invalidUserJwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_UNKNOWN")
    sendAuthenticatedPostRequestWithBody("/recalls", bookRecallRequest, invalidUserJwt)
      .expectStatus().isUnauthorized
  }

  @Test
  fun `can store recall with 201 response`() {
    val result = authenticatedPostRequest(
      "/recalls",
      bookRecallRequest,
      jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    )
    assertThat(result.nomsNumber, equalTo(nomsNumber))
    assertNotNull(result.id)
  }

  private fun authenticatedPostRequest(
    path: String,
    request: Any,
    userJwt: String
  ): BookRecallResponse {
    val responseType = object : ParameterizedTypeReference<BookRecallResponse>() {}
    return sendAuthenticatedPostRequestWithBody(path, request, userJwt)
      .expectStatus().isCreated
      .expectBody(responseType)
      .returnResult()
      .responseBody!!
  }
}
