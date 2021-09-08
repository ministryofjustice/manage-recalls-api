package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service

@Service
class DossierService(
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getDossier(recallId: RecallId): Mono<ByteArray> {
    return revocationOrderService.getRevocationOrder(recallId)
  }
}
