package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.alphanumeric
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.kebabAlphanumeric
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.maxLength
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.minLength
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.notBlank
import java.util.UUID

/*
  Tiny wrapper types:
  If the type will be used json it will need custom Jackson serializer/deserializer: see `RestConfiguration`.
  It will also need adding to the `OpenApiConfiguration` to ensure it is presented correctly in the swagger docs
  (until we can figure out how to get SpringDoc to do that otherwise).
  If it is to be used as a PathVariable it will need a customer Converter: see `DomainConverters`.
 */

class NomsNumber(value: String) : Validated<String>(value, notBlank, alphanumeric)
class CroNumber(value: String) : Validated<String>(value, notBlank)
class RecallId(value: UUID) : Validated<UUID>(value)
class DocumentId(value: UUID) : Validated<UUID>(value)
class MissingDocumentsRecordId(value: UUID) : Validated<UUID>(value)
class LastKnownAddressId(value: UUID) : Validated<UUID>(value)
class RescindRecordId(value: UUID) : Validated<UUID>(value)
class NoteId(value: UUID) : Validated<UUID>(value)
class UserId(value: UUID) : Validated<UUID>(value)
class FirstName(value: String) : Validated<String>(value, notBlank)
class MiddleNames(value: String) : Validated<String>(value, notBlank)
class LastName(value: String) : Validated<String>(value, notBlank)
class FullName(value: String) : Validated<String>(value, notBlank)
class Email(value: String) : Validated<String>(value, notBlank)
class PhoneNumber(value: String) : Validated<String>(value, notBlank)
class PrisonName(value: String) : Validated<String>(value, notBlank)
class PrisonId(value: String) : Validated<String>(value, notBlank, alphanumeric, 2.minLength, 6.maxLength)
class CourtId(value: String) : Validated<String>(value, notBlank, alphanumeric, 2.minLength, 6.maxLength)
class CourtName(value: String) : Validated<String>(value, notBlank)
class PoliceForceId(value: String) : Validated<String>(value, notBlank, kebabAlphanumeric, 2.minLength, 64.maxLength)
class PoliceForceName(value: String) : Validated<String>(value, notBlank)
class FieldName(value: String) : Validated<String>(value, notBlank)
class FieldPath(value: String) : Validated<String>(value, notBlank)
class ColumnName(value: String) : Validated<String>(value, notBlank)
class WarrantReferenceNumber(value: String) : Validated<String>(value, notBlank)

fun <T : Validated<UUID>> ((UUID) -> T).random() = this(UUID.randomUUID())

object ValidationRules {
  val notBlank: Rule<String> get() = { it.isNotBlank() }
  val alphanumeric: Rule<String> get() = { it.all(Char::isLetterOrDigit) }
  val kebabAlphanumeric: Rule<String> get() = { str -> str.all { ch -> ch.isLetterOrDigit() || ch == '-' } }
  private val IntRange.constrainLength: Rule<String> get() = { contains(it.length) }
  val Int.minLength: Rule<String> get() = (this..Integer.MAX_VALUE).constrainLength
  val Int.maxLength: Rule<String> get() = (0..this).constrainLength
}

typealias Rule<T> = (T) -> Boolean

open class Validated<T : Comparable<T>>(
  val value: T,
  vararg rules: Rule<T>,
  private val asString: String = value.toString()
) : Comparable<Validated<T>> {
  init {
    rules.find { !it(value) }?.apply {
      throw IllegalArgumentException("'$asString' violated ${this@Validated::class.java.name} rule")
    }
  }

  override fun toString() = value.toString()

  override fun hashCode() = value.hashCode()

  override fun compareTo(other: Validated<T>): Int = value.compareTo(other.value)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Validated<*>

    if (value != other.value) return false

    return true
  }
}
