package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.CascadeType.ALL
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "recall")
data class Recall(
  @Id
  val id: UUID,

  @Column(name = "noms_number", nullable = false)
  @Convert(converter = NomsNumberJpaConverter::class)
  val nomsNumber: NomsNumber,

  @Column(name = "revocation_order_doc_s3_key")
  val revocationOrderDocS3Key: UUID? = null,

  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet(),

  @Enumerated(STRING)
  @Column(name = "recall_type")
  val recallType: RecallType? = null,

  @Column(name = "agree_with_recall_recommendation")
  val agreeWithRecallRecommendation: Boolean? = null,

  @Enumerated(STRING)
  @Column(name = "recall_length")
  val recallLength: RecallLength? = null,

  @Column(name = "last_release_prison")
  val lastReleasePrison: String? = null,

  @Column(name = "last_release_date_time")
  val lastReleaseDateTime: ZonedDateTime? = null,

  @Column(name = "recall_email_received_datetime")
  val recallEmailReceivedDateTime: ZonedDateTime? = null,

  @Column(name = "local_police_service")
  val localPoliceService: String? = null,

  @Column(name = "contraband")
  val contrabandDetail: String? = null,

  @Column(name = "vulnerability_diversity")
  val vulnerabilityDiversityDetail: String? = null,

  @Enumerated(STRING)
  @Column(name = "mappa_level")
  val mappaLevel: MappaLevel? = null,

) {
  constructor(
    recallId: RecallId,
    nomsNumber: NomsNumber,
    revocationOrderDocS3Key: UUID? = null,
    documents: Set<RecallDocument> = emptySet(),
    recallType: RecallType? = null,
    agreeWithRecallRecommendation: Boolean? = null,
    recallLength: RecallLength? = null,
    lastReleasePrison: String? = null,
    lastReleaseDateTime: ZonedDateTime? = null,
    recallEmailReceivedDateTime: ZonedDateTime? = null,
    localPoliceService: String? = null,
    contraband: String? = null,
    vulnerabilityDiversity: String? = null,
    mappaLevel: MappaLevel? = null,
  ) :
    this(
      recallId.value,
      nomsNumber,
      revocationOrderDocS3Key,
      documents,
      recallType,
      agreeWithRecallRecommendation,
      recallLength,
      lastReleasePrison,
      lastReleaseDateTime,
      recallEmailReceivedDateTime,
      localPoliceService,
      contraband,
      vulnerabilityDiversity,
      mappaLevel
    )

  fun recallId() = RecallId(id)
}

class NomsNumberJpaConverter : CustomJpaConverter<NomsNumber, String>({ it.value }, ::NomsNumber)

abstract class CustomJpaConverter<IN, OUT>(private val toDbFn: (IN) -> OUT, private val fromDbFn: (OUT) -> IN) :
  AttributeConverter<IN, OUT> {
  override fun convertToDatabaseColumn(value: IN): OUT = toDbFn(value)

  override fun convertToEntityAttribute(value: OUT): IN = fromDbFn(value)
}
