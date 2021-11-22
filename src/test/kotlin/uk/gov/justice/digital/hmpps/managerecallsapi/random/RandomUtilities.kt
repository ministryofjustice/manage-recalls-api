package uk.gov.justice.digital.hmpps.managerecallsapi.random

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Validated
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

internal fun fullyPopulatedRecall(recallId: RecallId = ::RecallId.random()): Recall =
  fullyPopulatedInstance<Recall>().let { recall ->
    // ensure recall length is valid for the random sentencing info as it is calculated on the fly
    recall.copy(
      id = recallId.value,
      recallLength = recall.sentencingInfo?.calculateRecallLength(),
      documents = recall.documents.map {
        document ->
        document.copy(
          recallId = recallId.value,
          // ensure document version is valid verus category
          version = if (document.category.versioned) Random.nextInt() else null
        )
      }.toSet()
    )
  }

internal inline fun <reified T : Any> fullyPopulatedInstance(): T =
  T::class.createRandomInstance() as T

internal fun KClass<*>.createRandomInstance(type: KType = this.createType()): Any {
  createStandardInstance(type)?.let {
    return it
  }
  val constructor = constructors.last()
  val fieldValues = constructor.parameters
    .map { createRandomInstanceForParameter(it.type, this, type) }
    .toTypedArray()
  return constructor.call(*fieldValues)
}

private fun createRandomInstanceForParameter(paramType: KType, kclass: KClass<*>, type: KType): Any =
  when (val classifier = paramType.classifier) {
    is KClass<*> -> classifier.createRandomInstance(paramType)
    is KTypeParameter -> {
      val typeParameterId = kclass.typeParameters.indexOfFirst { it.name == classifier.name }
      val parameterType = type.arguments[typeParameterId].type ?: Any::class.createType()
      (parameterType.classifier as KClass<*>).createRandomInstance(paramType)
    }
    else -> throw Error("Type of the classifier $classifier is not supported")
  }

private fun KClass<*>.createStandardInstance(type: KType): Any? =
  if (this.isSubclassOf(Enum::class))
    this.java.enumConstants.random()
  else
    when (this) {
      Int::class -> Random.nextInt()
      UUID::class -> UUID.randomUUID()
      NomsNumber::class -> randomNoms()
      PrisonId::class -> randomPrisonId()
      CourtId::class -> randomCourtId()
      PoliceForceId::class -> randomPoliceForceId()
      Boolean::class -> Random.nextBoolean()
      LocalDate::class -> LocalDate.now()
      OffsetDateTime::class -> OffsetDateTime.now()
      String::class -> RandomStringUtils.randomAlphanumeric(25)
      Set::class, Collection::class -> makeRandomSet(this, type)
      else -> null
    }

private fun makeRandomSet(kclass: KClass<*>, type: KType): Set<Any> {
  val elemType = type.arguments[0].type!!
  return setOf(createRandomInstanceForParameter(elemType, kclass, type))
}

fun <T : Validated<UUID>> ((UUID) -> T).zeroes() = this(UUID(0, 0))
fun randomString(): String = RandomStringUtils.randomAlphanumeric(10)
fun randomNoms() = NomsNumber(RandomStringUtils.randomAlphanumeric(7))
fun randomPrisonId() = PrisonId(RandomStringUtils.randomAlphanumeric(6))
fun randomCourtId() = CourtId(RandomStringUtils.randomAlphanumeric(6))
fun randomPoliceForceId() = PoliceForceId(RandomStringUtils.randomAlphanumeric(6))
fun randomVersionedDocumentCategory() = RecallDocumentCategory.values().filter { it.versioned }.random()
fun randomUnVersionedDocumentCategory() = RecallDocumentCategory.values().filter { !it.versioned }.random()
fun randomAdultDateOfBirth(): LocalDate? {
  val age18 = LocalDate.now().minusYears(18)
  val endEpochDay = age18.toEpochDay()
  val startEpochDay = age18.minusYears(80).toEpochDay()
  val randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay)
  return LocalDate.ofEpochDay(randomDay)
}
