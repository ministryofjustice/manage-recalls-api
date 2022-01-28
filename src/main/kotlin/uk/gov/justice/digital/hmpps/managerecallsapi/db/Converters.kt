package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import javax.persistence.AttributeConverter

class FirstNameJpaConverter : CustomJpaConverter<FirstName, String>({ it.value }, ::FirstName)
class MiddleNamesJpaConverter : CustomJpaConverter<MiddleNames?, String?>({ it?.value }, { it?.let { MiddleNames(it) } })
class LastNameJpaConverter : CustomJpaConverter<LastName, String>({ it.value }, ::LastName)
class EmailJpaConverter : CustomJpaConverter<Email, String>({ it.value }, ::Email)
class PhoneNumberJpaConverter : CustomJpaConverter<PhoneNumber, String>({ it.value }, ::PhoneNumber)
class NomsNumberJpaConverter : CustomJpaConverter<NomsNumber, String>({ it.value }, ::NomsNumber)
class CroNumberJpaConverter : CustomJpaConverter<CroNumber, String>({ it.value }, ::CroNumber)
class PrisonIdJpaConverter : CustomJpaConverter<PrisonId?, String?>({ it?.value }, { it?.let { PrisonId(it) } })
class CourtIdJpaConverter : CustomJpaConverter<CourtId?, String?>({ it?.value }, { it?.let { CourtId(it) } })
class PoliceForceIdJpaConverter : CustomJpaConverter<PoliceForceId?, String?>({ it?.value }, { it?.let { PoliceForceId(it) } })
class WarrantReferenceNumberJpaConverter : CustomJpaConverter<WarrantReferenceNumber?, String?>({ it?.value }, { it?.let { WarrantReferenceNumber(it) } })

abstract class CustomJpaConverter<IN, OUT>(private val toDbFn: (IN) -> OUT, private val fromDbFn: (OUT) -> IN) :
  AttributeConverter<IN, OUT> {
  override fun convertToDatabaseColumn(value: IN): OUT = toDbFn(value)

  override fun convertToEntityAttribute(value: OUT): IN = fromDbFn(value)
}
