package uk.gov.justice.digital.hmpps.managerecallsapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import java.util.Base64

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class GenerateRevocationOrderController(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/generate-revocation-order")
  @ResponseBody
  fun generateRevocationOrder(): ResponseEntity<Pdf> {
    log.info("Generating revocation order")
    val pdfBase64Encoded = Base64.getEncoder().encodeToString(pdfDocumentGenerator.makePdf())
    return ResponseEntity.ok(Pdf(pdfBase64Encoded))
  }
}

data class Pdf(val content: String)
