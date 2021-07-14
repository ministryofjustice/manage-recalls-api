package uk.gov.justice.digital.hmpps.managerecallsapi.db

import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Recall(
  @Id
  val id: UUID,

  @Column(name = "noms_number", nullable = false)
  val nomsNumber: String,

  @Column(name = "revocation_order_doc_s3_key")
  val revocationOrderDocS3Key: UUID? = null
)
