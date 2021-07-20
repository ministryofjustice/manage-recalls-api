package uk.gov.justice.digital.hmpps.managerecallsapi.db

import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
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
  val nomsNumber: String,

  @Column(name = "revocation_order_doc_s3_key")
  val revocationOrderDocS3Key: UUID? = null,

  @OneToMany(cascade = [CascadeType.ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet()
)
