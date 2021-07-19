package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
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
  @Convert(converter = NomsNumberConverter::class)
  val nomsNumber: NomsNumber,

  @Column(name = "revocation_order_doc_s3_key")
  val revocationOrderDocS3Key: UUID? = null,

  @OneToMany(cascade = [CascadeType.ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet()
)

class NomsNumberConverter : CustomJpaConverter<NomsNumber, String>({ it.value }, ::NomsNumber)

abstract class CustomJpaConverter<IN, OUT>(private val toDbFn: (IN) -> OUT, private val toTypeFn: (OUT) -> IN) : AttributeConverter<IN, OUT> {
  override fun convertToDatabaseColumn(value: IN): OUT = toDbFn(value)

  override fun convertToEntityAttribute(value: OUT): IN = toTypeFn(value)
}
