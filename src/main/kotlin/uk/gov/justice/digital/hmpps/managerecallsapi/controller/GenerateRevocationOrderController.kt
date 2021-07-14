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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.util.Base64

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class GenerateRevocationOrderController(
  @Autowired private val revocationOrderService: RevocationOrderService
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("/generate-revocation-order")
  @ResponseBody
  fun generateRevocationOrder(@RequestBody request: RevocationOrderRequest): Mono<ResponseEntity<Pdf>> {
    return revocationOrderService.generateRevocationOrder(request.nomsNumber)
      .map {
        log.info("Generating revocation order")
        val pdfBase64Encoded = Base64.getEncoder().encodeToString(it)
        ResponseEntity.ok(Pdf(pdfBase64Encoded))
      }
  }
}

class RevocationOrderRequest(val nomsNumber: String)

data class Pdf(val content: String)
