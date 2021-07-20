package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.io.Serializable
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "recall")
data class Recall(
  @EmbeddedId
  val id: RecallKey,

  @Column(name = "noms_number", nullable = false)
  @Convert(converter = NomsNumberJpaConverter::class)
  val nomsNumber: NomsNumber,

  @Column(name = "revocation_order_doc_s3_key")
  val revocationOrderDocS3Key: UUID?,

  @OneToMany(cascade = [CascadeType.ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet()
) {
  constructor(recallId: RecallId, nomsNumber: NomsNumber, revocationOrderDocS3Key: UUID?, documents: Set<RecallDocument>) :
    this(recallId.toRecallKey(), nomsNumber, revocationOrderDocS3Key, documents)

  fun recallId() = id.toRecallId()
}

internal fun RecallId.toRecallKey(): RecallKey = RecallKey(value)
private fun RecallKey.toRecallId(): RecallId = RecallId(id)

@Embeddable
data class RecallKey(
  @Column(name = "id", nullable = false)
  val id: UUID
) : Serializable

class NomsNumberJpaConverter : CustomJpaConverter<NomsNumber, String>({ it.value }, ::NomsNumber)

abstract class CustomJpaConverter<IN, OUT>(private val toDbFn: (IN) -> OUT, private val toTypeFn: (OUT) -> IN) :
  AttributeConverter<IN, OUT> {
  override fun convertToDatabaseColumn(value: IN): OUT = toDbFn(value)

  override fun convertToEntityAttribute(value: OUT): IN = toTypeFn(value)
}
