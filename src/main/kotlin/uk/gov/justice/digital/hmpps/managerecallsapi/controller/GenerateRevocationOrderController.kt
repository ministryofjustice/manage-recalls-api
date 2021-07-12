package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.util.Base64

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class GenerateRevocationOrderController(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("/generate-revocation-order")
  @ResponseBody
  fun generateRevocationOrder(@RequestBody request: RevocationOrderRequest): Mono<ResponseEntity<Pdf>> {
    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(request.nomsNumber))
      .flatMap { prisoners ->
        val firstPrisoner = prisoners.first()
        val ctx = Context()

        val firstAndMiddleNames = String.format("%s %s", firstPrisoner.firstName, firstPrisoner.middleNames).trim()
        ctx.setVariable("firstNames", firstAndMiddleNames)
        ctx.setVariable("lastName", firstPrisoner.lastName)
        ctx.setVariable("dateOfBirth", firstPrisoner.dateOfBirth)
        ctx.setVariable("prisonNumber", firstPrisoner.bookNumber)
        ctx.setVariable("croNumber", firstPrisoner.croNumber)
        val populatedHtml = templateEngine.process("revocation-order", ctx)

        val details = listOf(
          HtmlDocumentDetail("index.html", populatedHtml),
          ClassPathDocumentDetail("logo.png", "/document/template/revocation-order/logo.png")
        )

        pdfDocumentGenerator.makePdf(details)
      }.map {
        log.info("Generating revocation order")
        val pdfBase64Encoded = Base64.getEncoder().encodeToString(it)
        ResponseEntity.ok(Pdf(pdfBase64Encoded))
      }
  }
}

class RevocationOrderRequest(val nomsNumber: String)

data class Pdf(val content: String)
