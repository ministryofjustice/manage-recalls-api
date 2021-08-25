package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

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

  @MockkBean
  private lateinit var recallDocumentService: RecallDocumentService

  private val nomsNumber = NomsNumber("123456")
  private val recallId = ::RecallId.random()
  private val aRecall = Recall(recallId, nomsNumber)
  private val bookRecallRequest = BookRecallRequest(nomsNumber)
  private val fileBytes = "content".toByteArray()
  private val fileContent = Base64.getEncoder().encodeToString(fileBytes)
  private val category = RecallDocumentCategory.PART_A_RECALL_REPORT
  private val addDocumentRequest = AddDocumentRequest(
    category = category.toString(),
    fileContent = fileContent
  )

  @Test
  fun `books a recall`() {

    every { recallRepository.save(any()) } returns aRecall

    val response =
      webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest).headers {
        it.withBearerAuthToken(jwtWithRoleManageRecalls())
      }
        .exchange()
        .expectStatus().isCreated
        .expectBody(RecallResponse::class.java)
        .returnResult()

    assertThat(
      response.responseBody,
      equalTo(RecallResponse(aRecall.recallId(), aRecall.nomsNumber, emptyList(), reasonsForRecall = emptyList()))
    )
  }

  @Test
  fun `returns all recalls`() {

    every { recallRepository.findAll() } returns listOf(aRecall)

    webTestClient.get().uri("/recalls").headers { it.withBearerAuthToken(jwtWithRoleManageRecalls()) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[:1].recallId").isEqualTo(recallId.toString())
      .jsonPath("$[:1].nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.revocationOrderId").doesNotExist()
  }

  @Test
  fun `gets a minimal recall`() {

    every { recallRepository.getByRecallId(recallId) } returns minimalRecall(recallId, nomsNumber)

    webTestClient.get().uri("/recalls/$recallId").headers { it.withBearerAuthToken(jwtWithRoleManageRecalls()) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.recallId").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.revocationOrderId").doesNotExist()
  }

  @Test
  fun `gets a fully populated recall`() {

    val recall = recallWithPopulatedFields(
      recallId,
      nomsNumber,
      documents = exampleDocuments(recallId)
    )
    every { recallRepository.getByRecallId(recallId) } returns recall

    webTestClient.get().uri("/recalls/$recallId").headers { it.withBearerAuthToken(jwtWithRoleManageRecalls()) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.recallId").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.documents.length()").isEqualTo(2)
      .jsonPath("$.documents[0].category").isEqualTo("PART_A_RECALL_REPORT")
      .jsonPath("$.documents[0].documentId").isNotEmpty
      .jsonPath("$.revocationOrderId").isNotEmpty
      .jsonPath("$.recallLength").isEqualTo("FOURTEEN_DAYS")
      .jsonPath("$.agreeWithRecallRecommendation").isEqualTo(recall.agreeWithRecallRecommendation.toString())
      .jsonPath("$.lastReleasePrison").isEqualTo(recall.lastReleasePrison.toString())
      .jsonPath("$.recallEmailReceivedDateTime").value(endsWith("Z"))
      .jsonPath("$.localPoliceForce").isEqualTo(recall.localPoliceForce!!)
      .jsonPath("$.contrabandDetail").isEqualTo(recall.contrabandDetail!!)
      .jsonPath("$.vulnerabilityDiversityDetail").isEqualTo(recall.vulnerabilityDiversityDetail!!)
      .jsonPath("$.mappaLevel").isEqualTo(MappaLevel.NA.name)
      .jsonPath("$.sentenceDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.licenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentencingCourt").isEqualTo(recall.sentencingInfo!!.sentencingCourt)
      .jsonPath("$.indexOffence").isEqualTo(recall.sentencingInfo!!.indexOffence)
      .jsonPath("$.conditionalReleaseDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.bookingNumber").isEqualTo(recall.bookingNumber!!)
      .jsonPath("$.probationOfficerName").isEqualTo(recall.probationInfo!!.probationOfficerName)
      .jsonPath("$.probationOfficerPhoneNumber").isEqualTo(recall.probationInfo!!.probationOfficerPhoneNumber)
      .jsonPath("$.probationOfficerEmail").isEqualTo(recall.probationInfo!!.probationOfficerEmail)
      .jsonPath("$.probationDivision").isEqualTo(recall.probationInfo!!.probationDivision.name)
      .jsonPath("$.authorisingAssistantChiefOfficer").isEqualTo(recall.probationInfo!!.authorisingAssistantChiefOfficer)
      .jsonPath("$.licenceConditionsBreached").isEqualTo(recall.licenceConditionsBreached!!)
      .jsonPath("$.reasonsForRecall.length()").isEqualTo(ReasonForRecall.values().size)
      .jsonPath("$.reasonsForRecall[0].reasonId").isNotEmpty
      .jsonPath("$.reasonsForRecall[0].reasonForRecall").isEqualTo("BREACH_EXCLUSION_ZONE")
  }

  @Test
  fun `gets a revocation order`() {
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

    val documentId = UUID.randomUUID()
    every { s3Service.uploadFile(any()) } returns documentId

    every { recallRepository.save(any()) } returns Recall(recallId, nomsNumber, documentId)

    val response = webTestClient.get().uri("/recalls/$recallId/revocationOrder").headers {
      it.withBearerAuthToken(jwtWithRoleManageRecalls())
    }
      .exchange()
      .expectStatus().isOk
      .expectBody(Pdf::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  @Test
  fun `adds a recall document`() {
    val recallId = UUID.randomUUID()
    val documentId = UUID.randomUUID()

    every { recallDocumentService.addDocumentToRecall(RecallId(recallId), fileBytes, category) } returns documentId

    webTestClient
      .post()
      .uri("/recalls/$recallId/documents")
      .bodyValue(addDocumentRequest)
      .headers { it.withBearerAuthToken(jwtWithRoleManageRecalls()) }
      .exchange()
      .expectStatus().isCreated
      .expectBody().jsonPath("$.documentId").isEqualTo(documentId.toString())
  }

  @Test
  fun `gets a recall document`() {
    val recallId = UUID.randomUUID()
    val documentId = UUID.randomUUID()
    val document = RecallDocument(
      id = documentId,
      recallId = recallId,
      category = RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    val bytes = "Hello".toByteArray()

    every { recallDocumentService.getDocument(RecallId(recallId), documentId) } returns Pair(document, bytes)

    webTestClient
      .get()
      .uri("/recalls/$recallId/documents/$documentId")
      .headers { it.withBearerAuthToken(jwtWithRoleManageRecalls()) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.documentId").isEqualTo(documentId.toString())
      .jsonPath("$.category").isEqualTo(RecallDocumentCategory.PART_A_RECALL_REPORT.toString())
      .jsonPath("$.content").isEqualTo(Base64.getEncoder().encodeToString(bytes))
  }

  private fun jwtWithRoleManageRecalls() = testJwt("ROLE_MANAGE_RECALLS")
}
