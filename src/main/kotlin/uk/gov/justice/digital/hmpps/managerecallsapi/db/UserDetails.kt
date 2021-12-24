package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_details")
data class UserDetails(
  @Id
  val id: UUID,
  @Column(nullable = false)
  @Convert(converter = FirstNameJpaConverter::class)
  val firstName: FirstName,
  @Column(nullable = false)
  @Convert(converter = LastNameJpaConverter::class)
  val lastName: LastName,
  @Column(nullable = false)
  val signature: String,
  @Column(nullable = false)
  @Convert(converter = EmailJpaConverter::class)
  val email: Email,
  @Column(nullable = false)
  @Convert(converter = PhoneNumberJpaConverter::class)
  val phoneNumber: PhoneNumber,
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val caseworkerBand: CaseworkerBand,
  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(
    userId: UserId,
    firstName: FirstName,
    lastName: LastName,
    signature: String,
    email: Email,
    phoneNumber: PhoneNumber,
    band: CaseworkerBand,
    createdDateTime: OffsetDateTime
  ) : this(
    userId.value,
    firstName,
    lastName,
    signature,
    email,
    phoneNumber,
    band,
    createdDateTime
  )

  fun userId() = UserId(id)

  fun personName() = PersonName(this.firstName, lastName = this.lastName)

  fun fullName(): FullName = personName().firstAndLastName()
}

enum class CaseworkerBand {
  THREE,
  FOUR_PLUS
}
