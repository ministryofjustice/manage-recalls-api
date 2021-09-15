package uk.gov.justice.digital.hmpps.managerecallsapi.random

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

internal fun fullyPopulatedRecall(): Recall = fullyPopulatedInstance<Recall>().let {
  // ensure recall length is valid for the random sentencing info as it is calculated on the fly
  it.copy(recallLength = it.sentencingInfo?.calculateRecallLength())
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
      val typeParameterName = classifier.name
      val typeParameterId = kclass.typeParameters.indexOfFirst { it.name == typeParameterName }
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
