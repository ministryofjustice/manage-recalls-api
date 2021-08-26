import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
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
import kotlin.random.Random

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
  nomsNumber: NomsNumber
) = Recall(
  recallId, nomsNumber,
  revocationOrderId = UUID.randomUUID(),
  documents = exampleDocuments(recallId),
  recallType = RecallType.FIXED,
  recallLength = RecallLength.values().random(),
  lastReleasePrison = randomAlphanumeric(500),
  lastReleaseDate = LocalDate.now(),
  recallEmailReceivedDateTime = dateTimeNow(),
  localPoliceForce = randomAlphanumeric(500),
  contrabandDetail = randomAlphanumeric(500),
  vulnerabilityDiversityDetail = randomAlphanumeric(500),
  mappaLevel = MappaLevel.NA,
  sentencingInfo = SentencingInfo(
    LocalDate.now(),
    LocalDate.now(),
    LocalDate.now(),
    randomAlphanumeric(500),
    randomAlphanumeric(500),
    SentenceLength(1, 2, 3),
    LocalDate.now()
  ),
  bookingNumber = randomAlphanumeric(500),
  probationInfo = ProbationInfo(
    randomAlphanumeric(500),
    randomAlphanumeric(500),
    randomAlphanumeric(500),
    ProbationDivision.NORTH_EAST,
    randomAlphanumeric(500)
  ),
  licenceConditionsBreached = randomAlphanumeric(500),
  reasonsForRecall = ReasonForRecall.values().toSortedSet(compareBy { it.name }),
  reasonsForRecallOtherDetail = randomAlphanumeric(500),
  agreeWithRecall = AgreeWithRecall.values().random(),
  agreeWithRecallDetail = randomAlphanumeric(500),
  currentPrison = randomAlphanumeric(500)
)

fun exampleDocuments(recallId: RecallId): Set<RecallDocument> {
  val partA = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = PART_A_RECALL_REPORT)
  val license = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = LICENCE)
  return setOf(partA, license)
}
