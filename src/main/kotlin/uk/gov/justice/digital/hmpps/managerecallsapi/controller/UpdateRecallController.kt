package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.ZonedDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UpdateRecallController(
  @Autowired private val recallRepository: RecallRepository
) {

  @PatchMapping("/recalls/{recallId}")
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest
  ): ResponseEntity<RecallResponse> =
    ResponseEntity.ok(
      recallRepository.getByRecallId(recallId).let {
        it.copy(
          recallType = FIXED,
          agreeWithRecallRecommendation = updateRecallRequest.agreeWithRecallRecommendation ?: it.agreeWithRecallRecommendation,
          recallLength = updateRecallRequest.recallLength ?: it.recallLength,
          recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime ?: it.recallEmailReceivedDateTime,
          lastReleasePrison = updateRecallRequest.lastReleasePrison ?: it.lastReleasePrison,
          lastReleaseDateTime = updateRecallRequest.lastReleaseDateTime ?: it.lastReleaseDateTime,
          localPoliceService = updateRecallRequest.localPoliceService ?: it.localPoliceService,
          contrabandDetail = updateRecallRequest.contrabandDetail ?: it.contrabandDetail,
          vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail ?: it.vulnerabilityDiversityDetail,
          mappaLevel = updateRecallRequest.mappaLevel ?: it.mappaLevel
        )
      }.let(recallRepository::save).toResponse()
    )
}

data class UpdateRecallRequest(
  val recallLength: RecallLength? = null,
  val agreeWithRecallRecommendation: Boolean? = null,
  val lastReleasePrison: String? = null,
  val lastReleaseDateTime: ZonedDateTime? = null,
  val recallEmailReceivedDateTime: ZonedDateTime? = null,
  val localPoliceService: String? = null,
  val contrabandDetail: String? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val mappaLevel: MappaLevel? = null
)

enum class RecallLength {
  FOURTEEN_DAYS,
  TWENTY_EIGHT_DAYS
}

enum class MappaLevel {
  NA,
  LEVEL_1,
  LEVEL_2,
  LEVEL_3,
  NOT_KNOWN,
  CONFIRMATION_REQUIRED
}

enum class RecallType {
  FIXED
}
