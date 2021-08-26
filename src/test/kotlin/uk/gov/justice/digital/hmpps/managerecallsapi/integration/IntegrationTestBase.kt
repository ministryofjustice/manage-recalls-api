package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

  @MockkBean
  lateinit var s3Service: S3Service

  @Autowired
  lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  lateinit var prisonerOffenderSearch: PrisonerOffenderSearchMockServer

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  lateinit var hmppsAuthMockServer: HmppsAuthMockServer

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @BeforeAll
  fun startMocks() {
    prisonerOffenderSearch.start()
    hmppsAuthMockServer.start()
  }

  @AfterAll
  fun stopMocks() {
    prisonerOffenderSearch.stop()
    hmppsAuthMockServer.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    prisonerOffenderSearch.resetAll()
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
  }

  protected fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)

  protected final inline fun <reified T> sendAuthenticatedPostRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.post().sendAuthenticatedRequestWithBody(path, request)

  protected final inline fun <reified T> sendAuthenticatedPatchRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.patch().sendAuthenticatedRequestWithBody(path, request)

  protected final inline fun <reified T> WebTestClient.RequestBodyUriSpec.sendAuthenticatedRequestWithBody(
    path: String,
    request: T,
    userJwt: String = testJwt("ROLE_MANAGE_RECALLS")
  ): WebTestClient.ResponseSpec =
    this.uri(path)
      .body(Mono.just(request), T::class.java)
      .headers {
        it.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        it.withBearerAuthToken(userJwt)
      }
      .exchange()

  fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")

  protected fun randomString() = UUID.randomUUID().toString()

  protected fun randomBoolean() = Random.nextBoolean()

  protected fun dateTimeNow() = OffsetDateTime.now()

  protected fun randomAdultDateOfBirth(): LocalDate? {
    val age18 = LocalDate.now().minusYears(18)
    val endEpochDay = age18.toEpochDay()
    val startEpochDay = age18.minusYears(80).toEpochDay()
    val randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay)
    return LocalDate.ofEpochDay(randomDay)
  }

  fun minimalRecall(recallId: RecallId, nomsNumber: NomsNumber) = Recall(recallId, nomsNumber)

  fun recallWithPopulatedFields(
    recallId: RecallId,
    nomsNumber: NomsNumber,
    recallLength: RecallLength = RecallLength.FOURTEEN_DAYS, // TODO AN: intending to remove this parameter also - in favour of a non-null default set below
    documents: Set<RecallDocument>
  ) = Recall(
    recallId, nomsNumber,
    revocationOrderId = UUID.randomUUID(),
    documents = documents,
    recallType = RecallType.FIXED,
    agreeWithRecallRecommendation = randomBoolean(),
    recallLength = recallLength,
    lastReleasePrison = randomString(),
    lastReleaseDate = LocalDate.now(),
    recallEmailReceivedDateTime = dateTimeNow(),
    localPoliceForce = randomString(),
    contrabandDetail = randomString(),
    vulnerabilityDiversityDetail = randomString(),
    mappaLevel = MappaLevel.NA,
    sentencingInfo = SentencingInfo(
      LocalDate.now(),
      LocalDate.now(),
      LocalDate.now(),
      randomString(),
      randomString(),
      SentenceLength(1, 2, 3),
      LocalDate.now()
    ),
    bookingNumber = randomString(),
    probationInfo = ProbationInfo(
      randomString(),
      randomString(),
      randomString(),
      ProbationDivision.NORTH_EAST,
      randomString()
    ),
    licenceConditionsBreached = "blah be de blah blue blah",
    reasonsForRecall = ReasonForRecall.values().toSortedSet(compareBy { it.name }),
    currentPrison = "MWI"
  )

  fun exampleDocuments(recallId: RecallId): Set<RecallDocument> {
    val partA = RecallDocument(
      id = UUID.randomUUID(),
      recallId = recallId.value,
      category = RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    val license =
      RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = RecallDocumentCategory.LICENCE)
    return setOf(partA, license)
  }
}
