package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.UUID

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator,
  @Autowired private val userDetailsService: UserDetailsService
) {

  fun createPdf(recallId: RecallId, userId: UserId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .flatMap { prisoners ->
        val revocationOrderHtml = revocationOrderGenerator.generateHtml(prisoners.first(), recall)

        val userDetails = userDetailsService.get(userId)

        pdfDocumentGenerationService.generatePdf(
          revocationOrderHtml,
          ClassPathDocumentDetail("revocation-order-logo.png"),
          InputStreamDocumentDetail("signature.jpg", ByteArrayInputStream(Base64.getDecoder().decode(userDetails.signature.toByteArray())))
        ).map { bytes ->
          UUID.randomUUID().let { revocationOrderId ->
            s3Service.uploadFile(revocationOrderId, bytes)
            recallRepository.save(recall.copy(revocationOrderId = revocationOrderId))
            bytes
          }
        }
      }
  }

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    return Mono.just(s3Service.downloadFile(recall.revocationOrderId!!))
  }
}
