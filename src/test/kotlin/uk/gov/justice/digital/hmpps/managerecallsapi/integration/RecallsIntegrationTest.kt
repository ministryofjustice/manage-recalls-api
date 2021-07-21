package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3BulkResponseEntity
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import java.util.stream.Stream

class RecallsIntegrationTest : IntegrationTestBase() {

  @BeforeAll
  fun startGotenberg() {
    gotenbergMockServer.start()
  }

  @AfterAll
  fun stopGotenberg() {
    gotenbergMockServer.stop()
  }

  @MockkBean
  private lateinit var recallRepository: RecallRepository

  private val nomsNumber = NomsNumber("123456")
  private val recallId = ::RecallId.random()
  private val aRecall = Recall(recallId, nomsNumber)
  private val bookRecallRequest = BookRecallRequest(nomsNumber)

  @Suppress("unused")
  private fun requestBodySpecs() = Stream.of(
    webTestClient.get().uri("/recalls"),
    webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest)
  )

  @ParameterizedTest
  @MethodSource("requestBodySpecs")
  fun `unauthorized when MANAGE_RECALLS role is missing`(requestBodySpec: RequestBodySpec) {
    val invalidUserJwt = testJwt("ROLE_UNKNOWN")
    requestBodySpec.headers { it.withBearerAuthToken(invalidUserJwt) }
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `books a recall`() {
    val jwt = testJwt("ROLE_MANAGE_RECALLS")

    every { recallRepository.save(any()) } returns aRecall

    val response =
      webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest).headers { it.withBearerAuthToken(jwt) }
        .exchange()
        .expectStatus().isCreated
        .expectBody(RecallResponse::class.java)
        .returnResult()

    assertThat(
      response.responseBody,
      equalTo(RecallResponse(aRecall.recallId(), aRecall.nomsNumber, null))
    )
  }

  @Test
  fun `returns all recalls`() {
    val jwt = testJwt("ROLE_MANAGE_RECALLS")

    every { recallRepository.findAll() } returns listOf(aRecall)

    webTestClient.get().uri("/recalls").headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[:1].id").isEqualTo(recallId.toString())
      .jsonPath("$[:1].nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.revocationOrderId").doesNotExist()
  }

  @Test
  fun `gets a recall`() {
    val jwt = testJwt("ROLE_MANAGE_RECALLS")

    every { recallRepository.getByRecallId(recallId) } returns aRecall

    webTestClient.get().uri("/recalls/$recallId").headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.id").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.revocationOrderId").doesNotExist()
  }

  @Test
  fun `get a revocation order`() {
    val jwt = testJwt("ROLE_MANAGE_RECALLS")
    val expectedPdf = "Expected Generated PDF".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { recallRepository.getByRecallId(recallId) } returns aRecall

    val firstName = "Natalia"
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = firstName,
          lastName = "Oskina",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )

    gotenbergMockServer.stubPdfGeneration(expectedPdf, firstName)

    val revocationOrderS3Key = UUID.randomUUID()
    every { s3Service.uploadFile(any()) } returns
      S3BulkResponseEntity(
        "bucket-name",
        revocationOrderS3Key,
        true,
        200
      )

    every { recallRepository.save(any()) } returns Recall(recallId, nomsNumber, revocationOrderS3Key)

    val response = webTestClient.get().uri("/recalls/$recallId/revocationOrder").headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isOk
      .expectBody(Pdf::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }
}
