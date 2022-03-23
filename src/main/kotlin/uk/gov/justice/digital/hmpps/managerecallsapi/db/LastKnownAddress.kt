package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

enum class AddressSource {
  MANUAL,
  LOOKUP,
}

@Entity
@Table(name = "last_known_address")
data class LastKnownAddress(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  val line1: String,

  @Column
  val line2: String?,

  @Column
  val town: String,

  @Column(nullable = false)
  val postcode: String?,

  @Enumerated(STRING)
  @Column(nullable = false)
  val source: AddressSource,

  @Column(nullable = false)
  val index: Int,

  @Column(nullable = false)
  val createdByUserId: UUID,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(
    id: LastKnownAddressId,
    recallId: RecallId,
    line1: String,
    line2: String?,
    town: String,
    postcode: String?,
    source: AddressSource,
    index: Int,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime
  ) :
    this(
      id.value, recallId.value, line1, line2, town, postcode, source, index, createdByUserId.value, createdDateTime
    )

  fun id() = LastKnownAddressId(id)
  fun recallId() = RecallId(recallId)
  fun createdByUserId() = UserId(createdByUserId)
}
