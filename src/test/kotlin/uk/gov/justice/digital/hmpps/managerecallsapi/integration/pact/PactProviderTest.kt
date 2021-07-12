package uk.gov.justice.digital.hmpps.managerecallsapi.integration.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import org.apache.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate

@PactFilter(value = ["^((?!unauthorized).)*\$"])
class PactProviderAuthorizedTest : PactProviderTestBase() {
  private val nomsNumber = "A1234AA"
  private val prisonerSearchRequest = PrisonerSearchRequest(nomsNumber)
  private fun validJwt() = jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    request.removeHeaders(AUTHORIZATION)
    request.addHeader(AUTHORIZATION, "Bearer ${validJwt()}")
    pactContext.verifyInteraction()
  }

  @State("prisoner exists for NOMS number")
  fun `prisoner exists for NOMS number`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      prisonerSearchRequest,
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber,
          firstName = "Bertie",
          lastName = "Badger",
          dateOfBirth = LocalDate.of(1990, 10, 30)
        )
      )
    )
  }

  @State("search by blank NOMS number")
  fun `search by blank NOMS number`() {
  }
}

@PactFilter(value = [".*unauthorized.*"])
class PactProviderUnauthorizedTest : PactProviderTestBase() {
  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    pactContext.verifyInteraction()
  }

  @State("unauthorized user accessToken")
  fun `unauthorized user accessToken`() {
  }
}

@ExtendWith(SpringExtension::class)
@VerificationReports(value = ["console"])
@Provider("manage-recalls-api")
@Consumer("manage-recalls-ui")
@PactBroker
abstract class PactProviderTestBase : IntegrationTestBase() {
  @LocalServerPort
  private val port = 0

  @BeforeEach
  fun before(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }
}
