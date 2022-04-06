package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import org.assertj.core.api.Assertions
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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddressRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.NoteRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReasonAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.CourtRegisterMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonRegisterMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import xyz.capybara.clamav.ClamavClient
import xyz.capybara.clamav.commands.scan.result.ScanResult
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("db-test")
@TestInstance(PER_CLASS)
abstract class ComponentTestBase(private val useRealGotenbergServer: Boolean = false) {

  @MockkBean
  protected lateinit var fixedClock: Clock

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
  protected lateinit var partBRecordRepository: PartBRecordRepository

  @Autowired
  protected lateinit var phaseRecordRepository: PhaseRecordRepository

  @Autowired
  protected lateinit var lastKnownAddressRepository: LastKnownAddressRepository

  @Autowired
  protected lateinit var noteRepository: NoteRepository

  @Autowired
  protected lateinit var rescindRecordRepository: RescindRecordRepository

  @Autowired
  protected lateinit var prisonerOffenderSearchMockServer: PrisonerOffenderSearchMockServer

  @Autowired
  protected lateinit var prisonApiMockServer: PrisonApiMockServer

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

  protected lateinit var fixedClockTime: OffsetDateTime

  @BeforeAll
  fun startMocks() {
    hmppsAuthMockServer.start()
    prisonerOffenderSearchMockServer.start()
    prisonApiMockServer.start()
    prisonRegisterMockServer.start()
    courtRegisterMockServer.start()
    if (!useRealGotenbergServer) gotenbergMockServer.start()
    setupUserDetailsFor(authenticatedClient.userId)
  }

  fun `delete all recalls`() {
    // Due to DB constraints, need to clear out the reasons before deleting the audit else the recallRepository delete
    // triggers the audit and you can not delete the recalls as they are referenced in the recall_reason_audit
    recallRepository.findAll().map { it.copy(reasonsForRecall = emptySet()) }
      .map { recallRepository.save(it, authenticatedClient.userId) }
    recallReasonAuditRepository.deleteAll()
    recallAuditRepository.deleteAll()
    missingDocumentsRecordRepository.deleteAll()
    phaseRecordRepository.deleteAll()
    partBRecordRepository.deleteAll()
    lastKnownAddressRepository.deleteAll()
    rescindRecordRepository.deleteAll()
    noteRepository.deleteAll()
    documentRepository.deleteAll()
    recallRepository.deleteAll()
  }

  @AfterAll
  fun stopMocks() {
    hmppsAuthMockServer.stop()
    prisonerOffenderSearchMockServer.stop()
    prisonApiMockServer.stop()
    prisonRegisterMockServer.stop()
    courtRegisterMockServer.stop()
    if (!useRealGotenbergServer) gotenbergMockServer.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
    prisonerOffenderSearchMockServer.resetAll()
    prisonApiMockServer.resetAll()
    prisonRegisterMockServer.resetAll()
    prisonRegisterMockServer.stubPrisons()
    courtRegisterMockServer.stubCourts()
    if (!useRealGotenbergServer) gotenbergMockServer.resetAll()
  }

  @BeforeEach
  fun setUpFixedClock() {
    val zone = ZoneId.of("UTC")
    val instant = Instant.parse("2022-02-04T14:15:43.682078Z")
    fixedClockTime = OffsetDateTime.ofInstant(instant, zone)
    every { fixedClock.instant() } returns instant
    every { fixedClock.zone } returns zone
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

  // Due to differences in rounding (trigger drops last 0 on nano-seconds) we need to allow some variance on OffsetDateTimes
  fun assertOffsetDateTimesEqual(actual: OffsetDateTime, expected: OffsetDateTime): AbstractOffsetDateTimeAssert<*> =
    Assertions.assertThat(actual).isCloseTo(expected, Assertions.within(1, ChronoUnit.MILLIS))!!
}
