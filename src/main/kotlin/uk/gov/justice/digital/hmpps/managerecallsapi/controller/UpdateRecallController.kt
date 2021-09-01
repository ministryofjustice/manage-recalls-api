package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UpdateRecallController(private val updateRecallService: UpdateRecallService, private val prisonValidationService: PrisonValidationService) {

  @PatchMapping("/recalls/{recallId}")
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest
  ): ResponseEntity<RecallResponse> =
    if (prisonValidationService.isValidPrison(updateRecallRequest.currentPrison)) {
      ResponseEntity.ok(
        updateRecallService.updateRecall(recallId, updateRecallRequest)
          .toResponse()
      )
    } else {
      ResponseEntity.badRequest().build()
    }
}

data class UpdateRecallRequest(
  val lastReleasePrison: String? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
  val contrabandDetail: String? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val mappaLevel: MappaLevel? = null,
  val sentenceDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val sentenceExpiryDate: LocalDate? = null,
  val sentencingCourt: String? = null,
  val indexOffence: String? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val sentenceLength: Api.SentenceLength? = null,
  val bookingNumber: String? = null,
  val probationOfficerName: String? = null,
  val probationOfficerPhoneNumber: String? = null,
  val probationOfficerEmail: String? = null,
  val probationDivision: ProbationDivision? = null,
  val authorisingAssistantChiefOfficer: String? = null,
  val licenceConditionsBreached: String? = null,
  val reasonsForRecall: Set<ReasonForRecall>? = null,
  val reasonsForRecallOtherDetail: String? = null,
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val currentPrison: String? = null,
  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val recallNotificationEmailSentDateTime: LocalDate? = null
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

enum class ProbationDivision {
  LONDON,
  MIDLANDS,
  NORTH_EAST,
  NORTH_WEST,
  SOUTH_EAST,
  SOUTH_WEST,
  SOUTH_WEST_AND_SOUTH_CENTRAL,
  WALES
}

enum class ReasonForRecall {
  BREACH_EXCLUSION_ZONE,
  ELM_BREACH_EXCLUSION_ZONE,
  ELM_BREACH_NON_CURFEW_CONDITION,
  ELM_FURTHER_OFFENCE,
  ELM_EQUIPMENT_TAMPER,
  ELM_FAILURE_CHARGE_BATTERY,
  FAILED_HOME_VISIT,
  FAILED_KEEP_IN_TOUCH,
  FAILED_RESIDE,
  FAILED_WORK_AS_APPROVED,
  POOR_BEHAVIOUR_ALCOHOL,
  POOR_BEHAVIOUR_FURTHER_OFFENCE,
  POOR_BEHAVIOUR_DRUGS,
  POOR_BEHAVIOUR_NON_COMPLIANCE,
  POOR_BEHAVIOUR_RELATIONSHIPS,
  TRAVELLING_OUTSIDE_UK,
  OTHER
}

enum class AgreeWithRecall {
  YES,
  NO_STOP
}
