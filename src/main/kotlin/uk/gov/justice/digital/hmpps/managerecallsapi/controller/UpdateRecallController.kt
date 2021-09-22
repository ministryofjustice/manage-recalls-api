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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
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
    if (prisonValidationService.isPrisonValidAndActive(updateRecallRequest.currentPrison) &&
      prisonValidationService.isPrisonValid(updateRecallRequest.lastReleasePrison)
    ) {
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
  val localDeliveryUnit: LocalDeliveryUnit? = null,
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
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val hasOtherPreviousConvictionMainName: Boolean? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  val assessedByUserId: UserId? = null,
  val bookedByUserId: UserId? = null,
  val dossierCreatedByUserId: UserId? = null
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

@Suppress("unused")
enum class LocalDeliveryUnit {
  CENTRAL_AUDIT_TEAM,
  CHANNEL_ISLANDS,
  ISLE_OF_MAN,
  NORTHERN_IRELAND,
  NOT_APPLICABLE,
  NOT_SPECIFIED,
  PS_BARKING_DAGENHAM_HAVERING,
  PS_BARNET,
  PS_BARNSLEY,
  PS_BATH_AND_NORTH_SOMERSET,
  PS_BEDFORDSHIRE,
  PS_BEXLEY,
  PS_BIRMINGHAM_CENTRAL_AND_SOUTH,
  PS_BIRMINGHAM_NORTH_AND_EAST,
  PS_BLACKBURN_AND_DARWEN,
  PS_BLACKPOOL,
  PS_BOLTON,
  PS_BRADFORD,
  PS_BRENT,
  PS_BRIGHTON_AND_EAST_SUSSEX,
  PS_BRISTOL_AND_SOUTH_GLOUCESTERSHIRE,
  PS_BROMLEY,
  PS_BUCKINGHAMSHIRE_M_KEYNES,
  PS_BURNLEY,
  PS_BURY,
  PS_CALDERDALE,
  PS_CAMBRIDGESHIRE_AND_PETERBOROUGH,
  PS_CAMDEN_ISLINGTON,
  PS_CARDIFF_AND_VALE,
  PS_CHESTER,
  PS_CHORLEY,
  PS_CORNWALL_AND_ISLES_OF_SCILLY,
  PS_COVENTRY,
  PS_CREWE,
  PS_CROYDON,
  PS_CUMBRIA,
  PS_CWM_TAF_MORGANNWG,
  PS_DERBY_CITY,
  PS_DERBYSHIRE,
  PS_DEVON_AND_TORBAY,
  PS_DONCASTER,
  PS_DORSET,
  PS_DUDLEY,
  PS_DURHAM,
  PS_DYFED_POWYS,
  PS_EALING,
  PS_EAST_BERKSHIRE,
  PS_EAST_CHESHIRE,
  PS_EAST_KENT,
  PS_EAST_LANCASHIRE,
  PS_EAST_RIDING,
  PS_ENFIELD,
  PS_ESSEX_NORTH,
  PS_ESSEX_SOUTH,
  PS_GATESHEAD,
  PS_GLOUCESTERSHIRE,
  PS_GREENWICH,
  PS_GWENT,
  PS_HACKNEY,
  PS_HALTON,
  PS_HAMMERSMITH_FULHAM,
  PS_HAMPSHIRE_NORTH_AND_EAST,
  PS_HARINGEY,
  PS_HARROW,
  PS_HARTLEPOOL,
  PS_HEREFORDSHIRE,
  PS_HERTFORDSHIRE,
  PS_HILLINGDON,
  PS_HOUNSLOW,
  PS_HULL,
  PS_KENSINGTON_CHELSEA_WESTMINSTER,
  PS_KINGSTON_RICHMOND,
  PS_KIRKLEES,
  PS_KNOWSLEY,
  PS_LAMBETH,
  PS_LANCASTER,
  PS_LEEDS,
  PS_LEICESTER,
  PS_LEICESTERSHIRE_AND_RUTLAND,
  PS_LEWISHAM,
  PS_LINCOLNSHIRE_EAST,
  PS_LINCOLNSHIRE_WEST,
  PS_LIVERPOOL_NORTH,
  PS_LIVERPOOL_SOUTH,
  PS_MACCLESFIELD,
  PS_MANCHESTER_NORTH,
  PS_MANCHESTER_SOUTH,
  PS_MERTON_SUTTON,
  PS_MIDDLESBROUGH,
  PS_NEWCASTLE_UPON_TYNE,
  PS_NEWHAM,
  PS_NORFOLK,
  PS_NORTH_DURHAM,
  PS_NORTH_EAST_LINCOLNSHIRE,
  PS_NORTH_LINCOLNSHIRE,
  PS_NORTH_TYNESIDE,
  PS_NORTH_WALES,
  PS_NORTH_YORKSHIRE,
  PS_NORTHAMPTONSHIRE,
  PS_NORTHUMBERLAND,
  PS_NORTHWICH,
  PS_NOTTINGHAM,
  PS_NOTTINGHAMSHIRE,
  PS_OLDHAM,
  PS_OXFORDSHIRE,
  PS_PLYMOUTH,
  PS_PORTSMOUTH_AND_IOW,
  PS_PRESTON,
  PS_REDBRIDGE,
  PS_REDCAR_CLEVELAND,
  PS_ROCHDALE,
  PS_ROTHERHAM,
  PS_SALFORD,
  PS_SANDWELL,
  PS_SEFTON,
  PS_SHEFFIELD,
  PS_SHROPSHIRE,
  PS_SKELMERSDALE,
  PS_SOLIHULL,
  PS_SOMERSET,
  PS_SOUTH_DURHAM,
  PS_SOUTH_TYNESIDE,
  PS_SOUTHAMPTON_EASTLEIGH_AND_NEW_FOREST,
  PS_SOUTHWARK,
  PS_ST_HELENS,
  PS_STAFFORDSHIRE,
  PS_STOCKPORT,
  PS_STOCKTON,
  PS_STOKE,
  PS_SUFFOLK,
  PS_SUNDERLAND,
  PS_SURREY,
  PS_SWANSEA_NEATH_AND_PORT_TALBOT,
  PS_SWINDON_AND_WILTSHIRE,
  PS_TAMESIDE,
  PS_TELFORD,
  PS_TOWER_HAMLETS,
  PS_TRAFFORD,
  PS_WAKEFIELD,
  PS_WALSALL,
  PS_WALTHAM_FOREST,
  PS_WANDSWORTH,
  PS_WARRINGTON,
  PS_WARWICKSHIRE,
  PS_WEST_BERKSHIRE,
  PS_WEST_CHESHIRE,
  PS_WEST_KENT,
  PS_WEST_SUSSEX,
  PS_WIGAN,
  PS_WIRRAL,
  PS_WOLVERHAMPTON,
  PS_WORCESTERSHIRE,
  PS_YORK,
  REGIONAL_CT_ADMIN,
  REPUBLIC_OF_IRELAND,
  SCOTLAND,
  YOT_SEE_COMMENTS
}

@Suppress("unused")
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
