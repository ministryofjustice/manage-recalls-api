package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
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

  private val fileBytes = "content".toByteArray()
  private val fileContent = Base64.getEncoder().encodeToString(fileBytes)
  private val category = RecallDocumentCategory.PART_A_RECALL_REPORT
  private val addDocumentRequest = AddDocumentRequest(
    category = category.toString(),
    fileContent = fileContent
  )

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
