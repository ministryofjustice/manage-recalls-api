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

enum class MappaLevel(val label: String) {
  NA("N/A"),
  LEVEL_1("Level 1"),
  LEVEL_2("Level 2"),
  LEVEL_3("Level 3"),
  NOT_KNOWN("Not Known"),
  CONFIRMATION_REQUIRED("Confirmation Required")
}

enum class RecallType {
  FIXED
}

@Suppress("unused")
enum class LocalDeliveryUnit(val label: String) {
  CENTRAL_AUDIT_TEAM("Central Audit Team"),
  CHANNEL_ISLANDS("Channel Islands"),
  ISLE_OF_MAN("Isle of Man"),
  NORTHERN_IRELAND("Northern Ireland"),
  NOT_APPLICABLE("Not applicable"),
  NOT_SPECIFIED("Not specified"),
  PS_BARKING_DAGENHAM_HAVERING("PS - Barking, Dagenham & Havering"),
  PS_BARNET("PS - Barnet"),
  PS_BARNSLEY("PS - Barnsley"),
  PS_BATH_AND_NORTH_SOMERSET("PS - Bath and North Somerset"),
  PS_BEDFORDSHIRE("PS - Bedfordshire"),
  PS_BEXLEY("PS - Bexley"),
  PS_BIRMINGHAM_CENTRAL_AND_SOUTH("PS - Birmingham Central and South"),
  PS_BIRMINGHAM_NORTH_AND_EAST("PS - Birmingham North and East"),
  PS_BLACKBURN_AND_DARWEN("PS - Blackburn and Darwen"),
  PS_BLACKPOOL("PS - Blackpool"),
  PS_BOLTON("PS - Bolton"),
  PS_BRADFORD("PS - Bradford"),
  PS_BRENT("PS - Brent"),
  PS_BRIGHTON_AND_EAST_SUSSEX("PS - Brighton and East Sussex"),
  PS_BRISTOL_AND_SOUTH_GLOUCESTERSHIRE("PS - Bristol and South Gloucestershire"),
  PS_BROMLEY("PS - Bromley"),
  PS_BUCKINGHAMSHIRE_M_KEYNES("PS - Buckinghamshire & M Keynes"),
  PS_BURNLEY("PS - Burnley"),
  PS_BURY("PS - Bury"),
  PS_CALDERDALE("PS - Calderdale"),
  PS_CAMBRIDGESHIRE_AND_PETERBOROUGH("PS - Cambridgeshire and Peterborough"),
  PS_CAMDEN_ISLINGTON("PS - Camden & Islington"),
  PS_CARDIFF_AND_VALE("PS - Cardiff and Vale"),
  PS_CHESTER("PS - Chester"),
  PS_CHORLEY("PS - Chorley"),
  PS_CORNWALL_AND_ISLES_OF_SCILLY("PS - Cornwall and Isles of Scilly"),
  PS_COVENTRY("PS - Coventry"),
  PS_CREWE("PS - Crewe"),
  PS_CROYDON("PS - Croydon"),
  PS_CUMBRIA("PS - Cumbria"),
  PS_CWM_TAF_MORGANNWG("PS - Cwm Taf Morgannwg"),
  PS_DERBY_CITY("PS - Derby City"),
  PS_DERBYSHIRE("PS - Derbyshire"),
  PS_DEVON_AND_TORBAY("PS - Devon and Torbay"),
  PS_DONCASTER("PS - Doncaster"),
  PS_DORSET("PS - Dorset"),
  PS_DUDLEY("PS - Dudley"),
  PS_DURHAM("PS - Durham"),
  PS_DYFED_POWYS("PS - Dyfed-Powys"),
  PS_EALING("PS - Ealing"),
  PS_EAST_BERKSHIRE("PS - East Berkshire"),
  PS_EAST_CHESHIRE("PS - East Cheshire"),
  PS_EAST_KENT("PS - East Kent"),
  PS_EAST_LANCASHIRE("PS - East Lancashire"),
  PS_EAST_RIDING("PS - East Riding"),
  PS_ENFIELD("PS - Enfield"),
  PS_ESSEX_NORTH("PS - Essex North"),
  PS_ESSEX_SOUTH("PS - Essex South"),
  PS_GATESHEAD("PS - Gateshead"),
  PS_GLOUCESTERSHIRE("PS - Gloucestershire"),
  PS_GREENWICH("PS - Greenwich"),
  PS_GWENT("PS - Gwent"),
  PS_HACKNEY("PS - Hackney"),
  PS_HALTON("PS - Halton"),
  PS_HAMMERSMITH_FULHAM("PS - Hammersmith & Fulham"),
  PS_HAMPSHIRE_NORTH_AND_EAST("PS - Hampshire North and East"),
  PS_HARINGEY("PS - Haringey"),
  PS_HARROW("PS - Harrow"),
  PS_HARTLEPOOL("PS - Hartlepool"),
  PS_HEREFORDSHIRE("PS - Herefordshire"),
  PS_HERTFORDSHIRE("PS - Hertfordshire"),
  PS_HILLINGDON("PS - Hillingdon"),
  PS_HOUNSLOW("PS - Hounslow"),
  PS_HULL("PS - Hull"),
  PS_KENSINGTON_CHELSEA_WESTMINSTER("PS - Kensington, Chelsea & Westminster"),
  PS_KINGSTON_RICHMOND("PS - Kingston & Richmond"),
  PS_KIRKLEES("PS - Kirklees"),
  PS_KNOWSLEY("PS - Knowsley"),
  PS_LAMBETH("PS - Lambeth"),
  PS_LANCASTER("PS - Lancaster"),
  PS_LEEDS("PS - Leeds"),
  PS_LEICESTER("PS - Leicester"),
  PS_LEICESTERSHIRE_AND_RUTLAND("PS - Leicestershire and Rutland"),
  PS_LEWISHAM("PS - Lewisham"),
  PS_LINCOLNSHIRE_EAST("PS - Lincolnshire East"),
  PS_LINCOLNSHIRE_WEST("PS - Lincolnshire West"),
  PS_LIVERPOOL_NORTH("PS - Liverpool North"),
  PS_LIVERPOOL_SOUTH("PS - Liverpool South"),
  PS_MACCLESFIELD("PS - Macclesfield"),
  PS_MANCHESTER_NORTH("PS - Manchester North"),
  PS_MANCHESTER_SOUTH("PS - Manchester South"),
  PS_MERTON_SUTTON("PS - Merton & Sutton"),
  PS_MIDDLESBROUGH("PS - Middlesbrough"),
  PS_NEWCASTLE_UPON_TYNE("PS - Newcastle Upon Tyne"),
  PS_NEWHAM("PS - Newham"),
  PS_NORFOLK("PS - Norfolk"),
  PS_NORTH_DURHAM("PS - North Durham"),
  PS_NORTH_EAST_LINCOLNSHIRE("PS - North East Lincolnshire"),
  PS_NORTH_LINCOLNSHIRE("PS - North Lincolnshire"),
  PS_NORTH_TYNESIDE("PS - North Tyneside"),
  PS_NORTH_WALES("PS - North Wales"),
  PS_NORTH_YORKSHIRE("PS - North Yorkshire"),
  PS_NORTHAMPTONSHIRE("PS - Northamptonshire"),
  PS_NORTHUMBERLAND("PS - Northumberland"),
  PS_NORTHWICH("PS - Northwich"),
  PS_NOTTINGHAM("PS - Nottingham"),
  PS_NOTTINGHAMSHIRE("PS - Nottinghamshire"),
  PS_OLDHAM("PS - Oldham"),
  PS_OXFORDSHIRE("PS - Oxfordshire"),
  PS_PLYMOUTH("PS - Plymouth"),
  PS_PORTSMOUTH_AND_IOW("PS - Portsmouth and IoW"),
  PS_PRESTON("PS - Preston"),
  PS_REDBRIDGE("PS - Redbridge"),
  PS_REDCAR_CLEVELAND("PS - Redcar Cleveland"),
  PS_ROCHDALE("PS - Rochdale"),
  PS_ROTHERHAM("PS - Rotherham"),
  PS_SALFORD("PS - Salford"),
  PS_SANDWELL("PS - Sandwell"),
  PS_SEFTON("PS - Sefton"),
  PS_SHEFFIELD("PS - Sheffield"),
  PS_SHROPSHIRE("PS - Shropshire"),
  PS_SKELMERSDALE("PS - Skelmersdale"),
  PS_SOLIHULL("PS - Solihull"),
  PS_SOMERSET("PS - Somerset"),
  PS_SOUTH_DURHAM("PS - South Durham"),
  PS_SOUTH_TYNESIDE("PS - South Tyneside"),
  PS_SOUTHAMPTON_EASTLEIGH_AND_NEW_FOREST("PS - Southampton, Eastleigh and New Forest"),
  PS_SOUTHWARK("PS - Southwark"),
  PS_ST_HELENS("PS - St Helens"),
  PS_STAFFORDSHIRE("PS - Staffordshire"),
  PS_STOCKPORT("PS - Stockport"),
  PS_STOCKTON("PS - Stockton"),
  PS_STOKE("PS - Stoke"),
  PS_SUFFOLK("PS - Suffolk"),
  PS_SUNDERLAND("PS - Sunderland"),
  PS_SURREY("PS - Surrey"),
  PS_SWANSEA_NEATH_AND_PORT_TALBOT("PS - Swansea, Neath and Port-Talbot"),
  PS_SWINDON_AND_WILTSHIRE("PS - Swindon and Wiltshire"),
  PS_TAMESIDE("PS - Tameside"),
  PS_TELFORD("PS - Telford"),
  PS_TOWER_HAMLETS("PS - Tower Hamlets"),
  PS_TRAFFORD("PS - Trafford"),
  PS_WAKEFIELD("PS - Wakefield"),
  PS_WALSALL("PS - Walsall"),
  PS_WALTHAM_FOREST("PS - Waltham Forest"),
  PS_WANDSWORTH("PS - Wandsworth"),
  PS_WARRINGTON("PS - Warrington"),
  PS_WARWICKSHIRE("PS - Warwickshire"),
  PS_WEST_BERKSHIRE("PS - West Berkshire"),
  PS_WEST_CHESHIRE("PS - West Cheshire"),
  PS_WEST_KENT("PS - West Kent"),
  PS_WEST_SUSSEX("PS - West Sussex"),
  PS_WIGAN("PS - Wigan"),
  PS_WIRRAL("PS - Wirral"),
  PS_WOLVERHAMPTON("PS - Wolverhampton"),
  PS_WORCESTERSHIRE("PS - Worcestershire"),
  PS_YORK("PS - York"),
  REGIONAL_CT_ADMIN("Regional CT Admin"),
  REPUBLIC_OF_IRELAND("Republic of Ireland"),
  SCOTLAND("Scotland",),
  YOT_SEE_COMMENTS("YOT - See Comments")
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
