package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomBoolean
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class UpdateRecallServiceTest {
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallService(recallRepository)

  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(recallId, NomsNumber("A9876ZZ"))
  private val today = LocalDate.now()

  private fun buildFullyPopulatedRecallRequest(): UpdateRecallRequest {
    val entity = UpdateRecallRequest::class.java
    val fields = entity.declaredFields
    val elements = Array<Any>(fields.size) { i ->
      when (fields[i].type) {
        java.lang.Boolean::class.java -> randomBoolean()
        LocalDate::class.java -> LocalDate.now()
        OffsetDateTime::class.java -> OffsetDateTime.now()
        MappaLevel::class.java -> MappaLevel.values().random()
        Api.SentenceLength::class.java -> Api.SentenceLength(1, 2, 3)
        ProbationDivision::class.java -> ProbationDivision.values().random()
        Set::class.java -> ReasonForRecall.values().toSet()
        AgreeWithRecall::class.java -> AgreeWithRecall.values().random()
        String::class.java -> randomString()
        else -> throw IllegalArgumentException("Unable to construct UpdateRecallRequest: Unknown field type [${fields[i].type}]")
      }
    }

    return entity.constructors.last().newInstance(*elements) as UpdateRecallRequest
  }

  private val fullyPopulatedUpdateRecallRequest = buildFullyPopulatedRecallRequest()

  private val fullyPopulatedRecallSentencingInfo = SentencingInfo(
    fullyPopulatedUpdateRecallRequest.sentenceDate!!,
    fullyPopulatedUpdateRecallRequest.licenceExpiryDate!!,
    fullyPopulatedUpdateRecallRequest.sentenceExpiryDate!!,
    fullyPopulatedUpdateRecallRequest.sentencingCourt!!,
    fullyPopulatedUpdateRecallRequest.indexOffence!!,
    SentenceLength(
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.years,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.months,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.days
    ),
    fullyPopulatedUpdateRecallRequest.conditionalReleaseDate
  )

  private val fullyPopulatedRecallWithoutDocuments = existingRecall.copy(
    recallType = FIXED,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    localPoliceForce = fullyPopulatedUpdateRecallRequest.localPoliceForce,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentencingInfo = fullyPopulatedRecallSentencingInfo,
    bookingNumber = fullyPopulatedUpdateRecallRequest.bookingNumber,
    probationInfo = ProbationInfo(
      fullyPopulatedUpdateRecallRequest.probationOfficerName!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerPhoneNumber!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerEmail!!,
      fullyPopulatedUpdateRecallRequest.probationDivision!!,
      fullyPopulatedUpdateRecallRequest.authorisingAssistantChiefOfficer!!,
    ),
    licenceConditionsBreached = fullyPopulatedUpdateRecallRequest.licenceConditionsBreached,
    reasonsForRecall = fullyPopulatedUpdateRecallRequest.reasonsForRecall!!.toSet(),
    reasonsForRecallOtherDetail = fullyPopulatedUpdateRecallRequest.reasonsForRecallOtherDetail,
    agreeWithRecall = fullyPopulatedUpdateRecallRequest.agreeWithRecall,
    agreeWithRecallDetail = fullyPopulatedUpdateRecallRequest.agreeWithRecallDetail,
    currentPrison = fullyPopulatedUpdateRecallRequest.currentPrison,
    additionalLicenceConditions = fullyPopulatedUpdateRecallRequest.additionalLicenceConditions,
    additionalLicenceConditionsDetail = fullyPopulatedUpdateRecallRequest.additionalLicenceConditionsDetail,
    differentNomsNumber = fullyPopulatedUpdateRecallRequest.differentNomsNumber,
    differentNomsNumberDetail = fullyPopulatedUpdateRecallRequest.differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = fullyPopulatedUpdateRecallRequest.recallNotificationEmailSentDateTime,
    dossierEmailSentDate = fullyPopulatedUpdateRecallRequest.dossierEmailSentDate,
    hasOtherPreviousConvictionMainName = fullyPopulatedUpdateRecallRequest.hasOtherPreviousConvictionMainName,
    hasDossierBeenChecked = fullyPopulatedUpdateRecallRequest.hasDossierBeenChecked,
    previousConvictionMainName = fullyPopulatedUpdateRecallRequest.previousConvictionMainName
  )

  private val fullyPopulatedRecallWithDocuments = fullyPopulatedRecallWithoutDocuments.copy(
    revocationOrderId = UUID.randomUUID(),
    documents = setOf(RecallDocument(UUID.randomUUID(), recallId.value, PART_A_RECALL_REPORT, randomString())),
  )

  @Test
  fun `can update recall with all fields populated`() {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(fullyPopulatedRecallWithoutDocuments) } returns fullyPopulatedRecallWithoutDocuments

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(fullyPopulatedRecallWithoutDocuments))
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    every { recallRepository.getByRecallId(recallId) } returns fullyPopulatedRecallWithDocuments
    every { recallRepository.save(fullyPopulatedRecallWithDocuments) } returns fullyPopulatedRecallWithDocuments

    val emptyUpdateRecallRequest = UpdateRecallRequest()
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest)

    assertThat(response, equalTo(fullyPopulatedRecallWithDocuments))
  }

  @Suppress("unused")
  private fun requestWithMissingMandatoryInfo(): Stream<UpdateRecallRequest>? {
    return Stream.of(
      recallRequestWithMandatorySentencingInfo(sentenceDate = null),
      recallRequestWithMandatorySentencingInfo(licenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentencingCourt = null),
      recallRequestWithMandatorySentencingInfo(indexOffence = null),
      recallRequestWithMandatorySentencingInfo(sentenceLength = null),
      recallRequestWithProbationInfo(probationOfficerName = null),
      recallRequestWithProbationInfo(probationOfficerPhoneNumber = null),
      recallRequestWithProbationInfo(probationOfficerEmail = null),
      recallRequestWithProbationInfo(probationDivision = null),
      recallRequestWithProbationInfo(authorisingAssistantChiefOfficer = null)
    )
  }

  @ParameterizedTest
  @MethodSource("requestWithMissingMandatoryInfo")
  fun `should not update recall if a mandatory field is not populated`(request: UpdateRecallRequest) {
    assertUpdateRecallDoesNotUpdate(request)
  }

  private fun assertUpdateRecallDoesNotUpdate(request: UpdateRecallRequest) {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    // TODO: don't set the recallType unless we need to
    val recallWithType = existingRecall.copy(recallType = FIXED)
    every { recallRepository.save(recallWithType) } returns recallWithType

    val response = underTest.updateRecall(recallId, request)

    assertThat(response, equalTo(recallWithType))
  }

  private fun recallRequestWithMandatorySentencingInfo(
    sentenceDate: LocalDate? = today,
    licenceExpiryDate: LocalDate? = today,
    sentenceExpiryDate: LocalDate? = today,
    sentencingCourt: String? = "court",
    indexOffence: String? = "index offence",
    sentenceLength: Api.SentenceLength? = Api.SentenceLength(10, 1, 1)
  ) = UpdateRecallRequest(
    sentenceDate = sentenceDate,
    licenceExpiryDate = licenceExpiryDate,
    sentenceExpiryDate = sentenceExpiryDate,
    sentencingCourt = sentencingCourt,
    indexOffence = indexOffence,
    sentenceLength = sentenceLength
  )

  private fun recallRequestWithProbationInfo(
    probationOfficerName: String? = "PON",
    probationOfficerPhoneNumber: String? = "07111111111",
    probationOfficerEmail: String? = "email@email.com",
    probationDivision: ProbationDivision? = ProbationDivision.NORTH_EAST,
    authorisingAssistantChiefOfficer: String? = "AACO"
  ) = UpdateRecallRequest(
    probationOfficerName = probationOfficerName,
    probationOfficerPhoneNumber = probationOfficerPhoneNumber,
    probationOfficerEmail = probationOfficerEmail,
    probationDivision = probationDivision,
    authorisingAssistantChiefOfficer = authorisingAssistantChiefOfficer
  )
}
