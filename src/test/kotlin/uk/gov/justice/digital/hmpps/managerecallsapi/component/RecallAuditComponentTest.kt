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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_EQUIPMENT_TAMPER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FAILURE_CHARGE_BATTERY
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
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
  private val today = LocalDate.now()

  private fun auditHistoryFieldValues(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(FieldPath("currentPrison"), "String or Enum", UpdateRecallRequest(currentPrison = PrisonId("ABC")), "ABC"),
      Arguments.of(FieldPath("contraband"), "Boolean", UpdateRecallRequest(contraband = true), true),
      Arguments.of(FieldPath("lastReleaseDate"), "LocalDate", UpdateRecallRequest(lastReleaseDate = today), today.toString()),
      Arguments.of(FieldPath("assessedByUserId"), "UUID", UpdateRecallRequest(assessedByUserId = authenticatedClient.userId), authenticatedClient.userId.toString()),
      Arguments.of(FieldPath("sentencingInfo.sentenceLength.sentenceYears"), "nested Integer", UpdateRecallRequest(sentenceLength = Api.SentenceLength(3, 0, 0), sentenceDate = today, licenceExpiryDate = today, indexOffence = "Offence 1", sentencingCourt = CourtId("ACCRYC"), sentenceExpiryDate = today), 3),
      Arguments.of(FieldPath("sentencingInfo.sentenceExpiryDate"), "nested LocalDate", UpdateRecallRequest(sentenceLength = Api.SentenceLength(3, 0, 0), sentenceDate = today, licenceExpiryDate = today, indexOffence = "Offence 1", sentencingCourt = CourtId("ACCRYC"), sentenceExpiryDate = today), today.toString()),
    )
  }

  @ParameterizedTest(name = "get {0} ({1} value) audit history")
  @MethodSource("auditHistoryFieldValues")
  fun `get audit history for single field`(
    fieldPath: FieldPath,
    fieldType: String,
    updateRequest: UpdateRecallRequest,
    expectedValue: Any
  ) {
    val savedRecall =
      authenticatedClient.bookRecall(
        BookRecallRequest(
          nomsNumber,
          FirstName("Brian"),
          null,
          LastName("Badgering"),
          CroNumber("1234/56A"),
          LocalDate.now()
        )
      )
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, updateRequest)

    val auditList = authenticatedClient.auditForField(recallId, fieldPath)

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(expectedValue))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get recallEmailReceivedDateTime (OffsetDateTime value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Brian"),
        null,
        LastName("Badgering"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    val recallId = savedRecall.recallId

    val recallEmailReceivedDateTime = OffsetDateTime.now()
    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(recallEmailReceivedDateTime = recallEmailReceivedDateTime))

    val auditList = authenticatedClient.auditForField(recallId, FieldPath("recallEmailReceivedDateTime"))

    assertThat(auditList.size, equalTo(1))
    assertOffsetDateTimesEqual(OffsetDateTime.parse(auditList[0].updatedValue.toString()), recallEmailReceivedDateTime)
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get reasonsForRecall (Array value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Brian"),
        null,
        LastName("Badgering"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(reasonsForRecall = setOf(OTHER, ELM_EQUIPMENT_TAMPER)))

    val auditList = authenticatedClient.auditForField(recallId, FieldPath("reasonsForRecall"))

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue == listOf(OTHER.name, ELM_EQUIPMENT_TAMPER.name))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get audit summary for a recall including reasons for recall`() {
    val savedRecall =
      authenticatedClient.bookRecall(
        BookRecallRequest(
          nomsNumber,
          FirstName("Brian"),
          null,
          LastName("Badgering"),
          CroNumber("1234/56A"),
          LocalDate.now()
        )
      )
    val recallId = savedRecall.recallId

    val auditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(
      auditList.map { it.fieldName.value } ==
        listOf(
          "lastUpdatedByUserId",
          "licenceNameCategory",
          "lastUpdatedDateTime",
          "nomsNumber",
          "createdByUserId",
          "createdDateTime",
          "reasonsForRecall",
          "firstName",
          "lastName"
        )
    )

    val lastUpdatedTimeAudit = auditList.first { it.fieldName.value == "lastUpdatedDateTime" }
    assertThat(lastUpdatedTimeAudit.fieldName, equalTo(FieldName("lastUpdatedDateTime")))
    assertThat(lastUpdatedTimeAudit.updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(lastUpdatedTimeAudit.auditCount, equalTo(1))
    assertOffsetDateTimesEqual(lastUpdatedTimeAudit.updatedDateTime, savedRecall.lastUpdatedDateTime)

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(contraband = true, reasonsForRecall = setOf(ELM_EQUIPMENT_TAMPER, ELM_FAILURE_CHARGE_BATTERY, OTHER)))

    val updatedAuditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(
      updatedAuditList.map { it.fieldName.value } ==
        listOf(
          "contraband",
          "lastUpdatedByUserId",
          "licenceNameCategory",
          "lastUpdatedDateTime",
          "nomsNumber",
          "createdByUserId",
          "createdDateTime",
          "reasonsForRecall",
          "recallType",
          "firstName",
          "lastName",
          "reasonsForRecall"
        )
    )

    assertThat(updatedAuditList[0].fieldName, equalTo(FieldName("contraband")))
    assertThat(updatedAuditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(updatedAuditList[0].auditCount, equalTo(1))
    assertOffsetDateTimesEqual(updatedAuditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)

    val lastUpdatedTimeUpdatedAudit = updatedAuditList.first { it.fieldName.value == "lastUpdatedDateTime" }
    assertThat(lastUpdatedTimeUpdatedAudit.updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(lastUpdatedTimeUpdatedAudit.auditCount, equalTo(2))
    assertOffsetDateTimesEqual(lastUpdatedTimeUpdatedAudit.updatedDateTime, updatedRecall.lastUpdatedDateTime)

    val reasonsForRecallAudit = updatedAuditList.first { it.fieldName.value == "reasonsForRecall" }
    assertThat(reasonsForRecallAudit.fieldPath, equalTo(FieldPath("reasonsForRecall")))
    assertThat(reasonsForRecallAudit.updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(reasonsForRecallAudit.auditCount, equalTo(1))
    assertOffsetDateTimesEqual(reasonsForRecallAudit.updatedDateTime, updatedRecall.lastUpdatedDateTime)

    // assert that audit still works when sentencing info - which has nested values - is updated
    authenticatedClient.updateRecall(recallId, UpdateRecallRequest(sentenceLength = Api.SentenceLength(3, 0, 0), sentenceDate = today, licenceExpiryDate = today, indexOffence = "Offence 1", sentencingCourt = CourtId("ACCRYC"), sentenceExpiryDate = today))
    val sentencingUpdateAuditList = authenticatedClient.auditSummaryForRecall(recallId)

    val sentenceAuditEntry = sentencingUpdateAuditList.first { it.fieldName.value == "sentenceYears" }
    assertThat(sentenceAuditEntry.fieldPath, equalTo(FieldPath("sentencingInfo.sentenceLength.sentenceYears")))
  }

  // Due to differences in rounding (trigger drops last 0 on nano-seconds) we need to allow some variance on OffsetDateTimes
  fun assertOffsetDateTimesEqual(actual: OffsetDateTime, expected: OffsetDateTime): AbstractOffsetDateTimeAssert<*> =
    assertThat(actual).isCloseTo(expected, within(1, ChronoUnit.MILLIS))!!
}
