package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallAuditComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  private fun auditHistoryFieldValues(): Stream<Arguments>? {
    val today = LocalDate.now()
    return Stream.of(
      Arguments.of(FieldName("currentPrison"), "String or Enum", UpdateRecallRequest(currentPrison = PrisonId("ABC")), "ABC"),
      Arguments.of(FieldName("contraband"), "Boolean", UpdateRecallRequest(contraband = true), true),
      Arguments.of(FieldName("lastReleaseDate"), "LocalDate", UpdateRecallRequest(lastReleaseDate = today), today.toString()),
      Arguments.of(FieldName("assessedByUserId"), "UUID", UpdateRecallRequest(assessedByUserId = authenticatedClient.userId), authenticatedClient.userId.toString()),
      Arguments.of(FieldName("sentencingInfo.sentenceLength.sentenceYears"), "nested Integer", UpdateRecallRequest(sentenceLength = Api.SentenceLength(3, 0, 0), sentenceDate = today, licenceExpiryDate = today, indexOffence = "Offence 1", sentencingCourt = CourtId("ACCRYC"), sentenceExpiryDate = today), 3),
      Arguments.of(FieldName("sentencingInfo.sentenceExpiryDate"), "nested LocalDate", UpdateRecallRequest(sentenceLength = Api.SentenceLength(3, 0, 0), sentenceDate = today, licenceExpiryDate = today, indexOffence = "Offence 1", sentencingCourt = CourtId("ACCRYC"), sentenceExpiryDate = today), today.toString()),
    )
  }

  @ParameterizedTest(name = "get {0} ({1} value) audit history")
  @MethodSource("auditHistoryFieldValues")
  fun `get audit history for single field`(
    fieldName: FieldName,
    fieldType: String,
    updateRequest: UpdateRecallRequest,
    expectedValue: Any
  ) {
    val savedRecall =
      authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, updateRequest)

    val auditList = authenticatedClient.auditForField(recallId, fieldName.value)

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(expectedValue))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get recallEmailReceivedDateTime (OffsetDateTime value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val recallEmailReceivedDateTime = OffsetDateTime.now()
    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(recallEmailReceivedDateTime = recallEmailReceivedDateTime))

    val auditList = authenticatedClient.auditForField(recallId, "recallEmailReceivedDateTime")

    assertThat(auditList.size, equalTo(1))
    assertOffsetDateTimesEqual(OffsetDateTime.parse(auditList[0].updatedValue.toString()), recallEmailReceivedDateTime)
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get audit summary for a recall`() {
    val savedRecall =
      authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val auditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(
      auditList.map { it.fieldName.value },
      equalTo(
        listOf(
          "lastUpdatedByUserId",
          "licenceNameCategory",
          "lastUpdatedDateTime",
          "nomsNumber",
          "createdByUserId",
          "createdDateTime",
          "id",
          "firstName",
          "lastName"
        )
      )
    )

    assertThat(auditList[2].fieldName, equalTo(FieldName("lastUpdatedDateTime")))
    assertThat(auditList[2].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(auditList[2].auditCount, equalTo(1))
    assertOffsetDateTimesEqual(auditList[2].updatedDateTime, savedRecall.lastUpdatedDateTime)

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(contraband = true))

    val updatedAuditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(
      updatedAuditList.map { it.fieldName.value },
      equalTo(
        listOf(
          "contraband",
          "lastUpdatedByUserId",
          "licenceNameCategory",
          "lastUpdatedDateTime",
          "nomsNumber",
          "createdByUserId",
          "createdDateTime",
          "id",
          "recallType",
          "firstName",
          "lastName"
        )
      )
    )

    assertThat(updatedAuditList[0].fieldName, equalTo(FieldName("contraband")))
    assertThat(updatedAuditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(updatedAuditList[0].auditCount, equalTo(1))
    assertOffsetDateTimesEqual(updatedAuditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)

    assertThat(updatedAuditList[3].fieldName, equalTo(FieldName("lastUpdatedDateTime")))
    assertThat(updatedAuditList[3].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(updatedAuditList[3].auditCount, equalTo(2))
    assertOffsetDateTimesEqual(updatedAuditList[3].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  // Due to differences in rounding (trigger drops last 0 on nano-seconds) we need to allow some variance on OffsetDateTimes
  fun assertOffsetDateTimesEqual(expected: OffsetDateTime, actual: OffsetDateTime): AbstractOffsetDateTimeAssert<*> =
    assertThat(expected).isCloseTo(actual, within(1, ChronoUnit.MILLIS))!!
}
