package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository
) {

  fun getRevocationOrder(recallId: RecallId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    if (recall.revocationOrderId == null) {
      return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
        .flatMap { prisoners ->
          val firstPrisoner = prisoners.first()
          val ctx = Context()

          val firstAndMiddleNames = String.format("%s %s", firstPrisoner.firstName, firstPrisoner.middleNames).trim()
          ctx.setVariable("firstNames", firstAndMiddleNames)
          ctx.setVariable("lastName", firstPrisoner.lastName)
          ctx.setVariable("dateOfBirth", firstPrisoner.dateOfBirth)
          ctx.setVariable("prisonNumber", firstPrisoner.bookNumber)
          ctx.setVariable("croNumber", firstPrisoner.croNumber)
          ctx.setVariable("licenseRevocationDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
          val populatedHtml = templateEngine.process("revocation-order", ctx)

          val details = listOf(
            HtmlDocumentDetail("index.html", populatedHtml),
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
