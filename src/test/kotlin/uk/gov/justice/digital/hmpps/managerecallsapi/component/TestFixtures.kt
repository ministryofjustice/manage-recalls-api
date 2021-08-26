import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall
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
import kotlin.random.Random

fun randomString() = UUID.randomUUID().toString()

fun randomBoolean() = Random.nextBoolean()

fun dateTimeNow() = OffsetDateTime.now()

fun randomAdultDateOfBirth(): LocalDate? {
  val age18 = LocalDate.now().minusYears(18)
  val endEpochDay = age18.toEpochDay()
  val startEpochDay = age18.minusYears(80).toEpochDay()
  val randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay)
  return LocalDate.ofEpochDay(randomDay)
}

fun minimalRecall(recallId: RecallId, nomsNumber: NomsNumber) = Recall(recallId, nomsNumber)

fun recallWithPopulatedFields(
  recallId: RecallId,
  nomsNumber: NomsNumber,
  recallLength: RecallLength = FOURTEEN_DAYS, // TODO AN: intending to remove this parameter also - in favour of a non-null default set below
  documents: Set<RecallDocument>
) = Recall(
  recallId, nomsNumber,
  revocationOrderId = UUID.randomUUID(),
  documents = documents,
  recallType = RecallType.FIXED,
  agreeWithRecallRecommendation = randomBoolean(),
  recallLength = recallLength,
  lastReleasePrison = randomString(),
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
  reasonsForRecallOtherDetail = randomString()
)

fun exampleDocuments(recallId: RecallId): Set<RecallDocument> {
  val partA = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = PART_A_RECALL_REPORT)
  val license = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = LICENCE)
  return setOf(partA, license)
}
