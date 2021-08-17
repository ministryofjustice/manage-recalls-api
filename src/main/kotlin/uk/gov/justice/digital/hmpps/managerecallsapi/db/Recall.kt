package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.CascadeType.ALL
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Embeddable
import javax.persistence.Embedded
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

  @Column(nullable = false)
  @Convert(converter = NomsNumberJpaConverter::class)
  val nomsNumber: NomsNumber,

  val revocationOrderId: UUID? = null,

  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet(),

  @Enumerated(STRING)
  val recallType: RecallType? = null,

  val agreeWithRecallRecommendation: Boolean? = null,

  @Enumerated(STRING)
  val recallLength: RecallLength? = null,

  val lastReleasePrison: String? = null,

  val lastReleaseDate: LocalDate? = null,

  val recallEmailReceivedDateTime: ZonedDateTime? = null,

  val localPoliceService: String? = null,

  val contrabandDetail: String? = null,

  val vulnerabilityDiversityDetail: String? = null,

  @Enumerated(STRING)
  val mappaLevel: MappaLevel? = null,

  @Embedded
  val sentencingInfo: SentencingInfo? = null,

) {
  constructor(
    recallId: RecallId,
    nomsNumber: NomsNumber,
    revocationOrderId: UUID? = null,
    documents: Set<RecallDocument> = emptySet(),
    recallType: RecallType? = null,
    agreeWithRecallRecommendation: Boolean? = null,
    recallLength: RecallLength? = null,
    lastReleasePrison: String? = null,
    lastReleaseDate: LocalDate? = null,
    recallEmailReceivedDateTime: ZonedDateTime? = null,
    localPoliceService: String? = null,
    contraband: String? = null,
    vulnerabilityDiversity: String? = null,
    mappaLevel: MappaLevel? = null,
    sentencingInfo: SentencingInfo? = null,
  ) :
    this(
      recallId.value,
      nomsNumber,
      revocationOrderId,
      documents,
      recallType,
      agreeWithRecallRecommendation,
      recallLength,
      lastReleasePrison,
      lastReleaseDate,
      recallEmailReceivedDateTime,
      localPoliceService,
      contraband,
      vulnerabilityDiversity,
      mappaLevel,
      sentencingInfo
    )

  fun recallId() = RecallId(id)
}

class NomsNumberJpaConverter : CustomJpaConverter<NomsNumber, String>({ it.value }, ::NomsNumber)

abstract class CustomJpaConverter<IN, OUT>(private val toDbFn: (IN) -> OUT, private val fromDbFn: (OUT) -> IN) :
  AttributeConverter<IN, OUT> {
  override fun convertToDatabaseColumn(value: IN): OUT = toDbFn(value)

  override fun convertToEntityAttribute(value: OUT): IN = fromDbFn(value)
}

@Embeddable
data class SentencingInfo(
  val sentenceDate: LocalDate,
  val licenceExpiryDate: LocalDate,
  val sentenceExpiryDate: LocalDate,
  val sentencingCourt: String,
  val indexOffence: String,
  val conditionalReleaseDate: LocalDate? = null,
)
