package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.commons.lang3.RandomUtils
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

fun ((UUID) -> RecallId).zeroes() = this(UUID(0, 0))

fun randomString() = randomAlphanumeric(500)

fun randomBoolean() = RandomUtils.nextBoolean()

fun randomNoms() = NomsNumber(randomAlphanumeric(7))

fun dateTimeNow() = OffsetDateTime.now()

fun randomAdultDateOfBirth(): LocalDate? {
  val age18 = LocalDate.now().minusYears(18)
  val endEpochDay = age18.toEpochDay()
  val startEpochDay = age18.minusYears(80).toEpochDay()
  val randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay)
  return LocalDate.ofEpochDay(randomDay)
}

fun recallWithPopulatedFields(
  recallId: RecallId,
  nomsNumber: NomsNumber,
  documents: Set<RecallDocument> = exampleDocuments(recallId)
) = Recall(
  recallId, nomsNumber,
  revocationOrderId = UUID.randomUUID(),
  documents = documents,
  recallType = RecallType.FIXED,
  recallLength = RecallLength.values().random(),
  lastReleasePrison = "WIN",
  lastReleaseDate = LocalDate.now(),
  recallEmailReceivedDateTime = dateTimeNow(),
  localPoliceForce = randomString(),
  contrabandDetail = randomString(),
  vulnerabilityDiversityDetail = randomString(),
  mappaLevel = MappaLevel.NA,
  sentencingInfo = SentencingInfo(
    LocalDate.now(),
    LocalDate.now(),
    LocalDate.now(),
    randomString(),
    randomString(),
    SentenceLength(1, 2, 3),
    LocalDate.now()
  ),
  bookingNumber = randomString(),
  probationInfo = ProbationInfo(
    randomString(),
    randomString(),
    randomString(),
    ProbationDivision.NORTH_EAST,
    randomString()
  ),
  licenceConditionsBreached = randomString(),
  reasonsForRecall = ReasonForRecall.values().toSortedSet(compareBy { it.name }),
  reasonsForRecallOtherDetail = randomString(),
  agreeWithRecall = AgreeWithRecall.values().random(),
  agreeWithRecallDetail = randomString(),
  currentPrison = "MWI",
  additionalLicenceConditions = randomBoolean(),
  additionalLicenceConditionsDetail = randomString(),
  differentNomsNumber = randomBoolean(),
  differentNomsNumberDetail = randomString(),
  recallNotificationEmailSentDateTime = dateTimeNow()
)

fun exampleDocuments(recallId: RecallId): Set<RecallDocument> {
  val partA = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = PART_A_RECALL_REPORT, randomString())
  val license = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = LICENCE, randomString())
  return setOf(partA, license)
}
