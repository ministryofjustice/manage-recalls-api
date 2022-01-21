package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReasonAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.CourtRegisterMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonRegisterMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import xyz.capybara.clamav.ClamavClient
import xyz.capybara.clamav.commands.scan.result.ScanResult
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("db-test")
@TestInstance(PER_CLASS)
abstract class ComponentTestBase(private val useRealGotenbergServer: Boolean = false) {

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  protected lateinit var hmppsAuthMockServer: HmppsAuthMockServer

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var recallRepository: RecallRepository

  @Autowired
  protected lateinit var recallAuditRepository: RecallAuditRepository

  @Autowired
  protected lateinit var recallReasonAuditRepository: RecallReasonAuditRepository

  @Autowired
  protected lateinit var userDetailsRepository: UserDetailsRepository

  @Autowired
  protected lateinit var documentRepository: DocumentRepository

  @Autowired
  protected lateinit var missingDocumentsRecordRepository: MissingDocumentsRecordRepository

  @Autowired
  protected lateinit var prisonerOffenderSearchMockServer: PrisonerOffenderSearchMockServer

  @Autowired
  protected lateinit var prisonRegisterMockServer: PrisonRegisterMockServer

  @Autowired
  protected lateinit var courtRegisterMockServer: CourtRegisterMockServer

  @Autowired
  protected lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  protected lateinit var s3Service: S3Service

  @Autowired
  protected lateinit var documentService: DocumentService

  @MockkBean
  protected lateinit var clamavClient: ClamavClient

  protected val authenticatedClient: AuthenticatedClient by lazy {
    AuthenticatedClient(webTestClient, jwtAuthenticationHelper)
  }

  @BeforeAll
  fun startMocks() {
    hmppsAuthMockServer.start()
    prisonerOffenderSearchMockServer.start()
    prisonRegisterMockServer.start()
    courtRegisterMockServer.start()
    if (!useRealGotenbergServer) gotenbergMockServer.start()
    setupUserDetailsFor(authenticatedClient.userId)
  }

  @AfterAll
  fun stopMocks() {
    hmppsAuthMockServer.stop()
    prisonerOffenderSearchMockServer.stop()
    prisonRegisterMockServer.stop()
    courtRegisterMockServer.stop()
    if (!useRealGotenbergServer) gotenbergMockServer.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
    prisonerOffenderSearchMockServer.resetAll()
    prisonRegisterMockServer.resetAll()
    prisonRegisterMockServer.stubPrisons()
    courtRegisterMockServer.stubCourts()
    if (!useRealGotenbergServer) gotenbergMockServer.resetAll()
  }

  @Configuration
  class TestConfig {
    @Bean
    fun cleanDatabase(): FlywayMigrationStrategy =
      FlywayMigrationStrategy { flyway ->
        flyway.clean()
        flyway.migrate()
      }
  }

  protected fun testJwt(userId: UserId, role: String) = authenticatedClient.testJwt(role, userId)

  protected fun unauthenticatedGet(path: String, expectedStatus: HttpStatus = OK): WebTestClient.BodyContentSpec =
    unauthenticatedGetResponse(path, expectedStatus)
      .expectBody()

  fun unauthenticatedGetResponse(path: String, expectedStatus: HttpStatus = OK) =
    webTestClient.get().uri(path)
      .headers { it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE) }
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)

  protected fun writeBase64EncodedStringToFile(fileName: String, content: String) {
    File(fileName).writeBytes(content.toBase64DecodedByteArray())
  }

  protected fun setupUserDetailsFor(userId: UserId) {
    userDetailsRepository.save(
      UserDetails(
        userId, FirstName("Bertie"), LastName("Badger"),
        base64EncodedFileContents("/signature.jpg"),
        Email("bertie@thebadgers.org"),
        PhoneNumber("09876543210"),
        CaseworkerBand.FOUR_PLUS,
        OffsetDateTime.now()
      )
    )
  }

  protected fun expectNoVirusesWillBeFound() {
    every { clamavClient.scan(any<InputStream>()) } returns ScanResult.OK
  }

  protected fun expectAVirusWillBeFound() {
    every { clamavClient.scan(any<InputStream>()) } returns ScanResult.VirusFound(mapOf())
  }

  protected fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, prisonerFirstName: String) {
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(
      nomsNumber,
      Prisoner(
        prisonerNumber = nomsNumber.value,
        croNumber = "CRO Num/456",
        firstName = prisonerFirstName,
        lastName = "Badger",
        dateOfBirth = LocalDate.of(2000, 1, 31),
        bookNumber = "Book Num 123"
      )
    )
  }
}
