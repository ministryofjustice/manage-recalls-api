package uk.gov.justice.digital.hmpps.managerecallsapi.integration.pact.provider

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.apache.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.time.LocalDate
import java.util.UUID

@PactFilter(value = ["^((?!unauthorized).)*\$"])
class ManagerRecallsUiAuthorizedPactTest : ManagerRecallsUiPactTestBase() {
  private val nomsNumber = NomsNumber("A1234AA")
  private val prisonerSearchRequest = PrisonerSearchRequest(nomsNumber)
  private fun validJwt() = jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")

  // TODO: Use a real database in service level integration tests (possibly not here though??)
  @MockkBean
  private lateinit var recallRepository: RecallRepository
  @MockkBean
  private lateinit var revocationOrderService: RevocationOrderService

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    request.removeHeaders(AUTHORIZATION)
    request.addHeader(AUTHORIZATION, "Bearer ${validJwt()}")
    pactContext.verifyInteraction()
  }

  @State("a prisoner exists for NOMS number")
  fun `a prisoner exists for NOMS number`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      prisonerSearchRequest,
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = "Bobby",
          middleNames = "John",
          lastName = "Badger",
          dateOfBirth = LocalDate.of(1999, 5, 28),
          gender = "Male",
          croNumber = "1234/56A",
          pncNumber = "98/7654Z"
        ),
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = "Bertie",
          middleNames = "Barry",
          lastName = "Badger",
          dateOfBirth = LocalDate.of(1990, 10, 30),
          gender = "Male",
          croNumber = "1234/56A",
          pncNumber = "98/7654Z"
        )
      )
    )
  }

  @State(
    "a search by blank NOMS number",
    "a create recall request with blank nomsNumber"
  )
  fun `no state required`() {
  }

  @State("a recall can be created")
  fun `a recall can be created`() {
    val aRecall = Recall(UUID.randomUUID(), nomsNumber)

    every { recallRepository.save(any()) } returns aRecall
  }

  @State("a list of recalls exists")
  fun `a list of recalls exists`() {
    every { recallRepository.findAll() } returns listOf(
      Recall(UUID.randomUUID(), nomsNumber),
      Recall(UUID.randomUUID(), NomsNumber("Z9876ZZ"))
    )
  }

  @State("a revocation order can be downloaded")
  fun `a revocation order can be downloaded`() {
    every { revocationOrderService.getRevocationOrder(any<UUID>()) } returns Mono.just("some pdf contents".toByteArray())
  }
}

@PactFilter(value = [".*unauthorized.*"])
class ManagerRecallsUiUnauthorizedPactTest : ManagerRecallsUiPactTestBase() {
  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    pactContext.verifyInteraction()
  }

  @State("an unauthorized user accessToken")
  fun `an unauthorized user accessToken`() {
  }
}

/**
 * This test is used to verify the contract between manage-recalls-api and manage-recalls-ui.
 * It defaults to verify the pact published with the 'main' tag.
 * To override this for verifying a different pact you can either specify the tag of a published pact file:
 * @PactBroker(
 *   consumerVersionSelectors = [ VersionSelector(tag = "pact") ]
 * )
 * Or specify a local pact file by removing @PactBroker and replacing with:
 * e.g. @PactFolder("../manage-recalls-ui/pact/pacts")
 */
@ExtendWith(SpringExtension::class)
@VerificationReports(value = ["console"])
@Provider("manage-recalls-api")
@Consumer("manage-recalls-ui")
@PactBroker
abstract class ManagerRecallsUiPactTestBase : IntegrationTestBase() {
  @LocalServerPort
  private val port = 0

  @BeforeEach
  fun before(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }
}
