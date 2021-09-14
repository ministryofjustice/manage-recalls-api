package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.StringDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    if (recall.revocationOrderId == null) {
      return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
        .flatMap { prisoners ->
          val revocationOrderHtml = revocationOrderGenerator.generateHtml(prisoners.first(), recall)

          val details = listOf(
            StringDocumentDetail("index.html", revocationOrderHtml),
            ClassPathDocumentDetail("revocation-order-logo.png", "/templates/images/revocation-order-logo.png")
          )

          pdfDocumentGenerator.makePdf(details).map { bytes ->
            UUID.randomUUID().let { revocationOrderId ->
              s3Service.uploadFile(revocationOrderId, bytes)
              recallRepository.save(recall.copy(revocationOrderId = revocationOrderId))
              bytes
            }
          }
        }
    } else {
      return Mono.just(s3Service.downloadFile(recall.revocationOrderId))
    }
  }
}
