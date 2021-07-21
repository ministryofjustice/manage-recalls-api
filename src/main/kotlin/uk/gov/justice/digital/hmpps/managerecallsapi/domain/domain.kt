package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.alphanumeric
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ValidationRules.notBlank
import java.util.UUID

/*
  If the type will be used json it will need custom Jackson serializer/deserializer:   RestConfiguration
  If it is to be used as a PathVariable it will need a customer Converter:  DomainConverters
 */

class NomsNumber(value: String) : Validated<String>(value, notBlank, alphanumeric)
class RecallId(value: UUID) : Validated<UUID>(value)

fun ((UUID) -> RecallId).random() = this(UUID.randomUUID())

object ValidationRules {
  val notBlank: Rule<String> get() = { it.isNotBlank() }
  val alphanumeric: Rule<String> get() = { it.all(Char::isLetterOrDigit) }
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
