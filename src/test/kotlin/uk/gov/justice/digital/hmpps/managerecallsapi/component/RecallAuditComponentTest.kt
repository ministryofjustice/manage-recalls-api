package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
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

class RecallAuditComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Test
  fun `get currentPrison (String or Enum value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(currentPrison = PrisonId("ABC")))

    val auditList = authenticatedClient.auditForField(recallId, "currentPrison")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo("ABC"))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get contraband (Boolean value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(contraband = true))

    val auditList = authenticatedClient.auditForField(recallId, "contraband")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(true))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get lastReleaseDate (LocalDate value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(lastReleaseDate = LocalDate.now()))

    val auditList = authenticatedClient.auditForField(recallId, "lastReleaseDate")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(LocalDate.now().toString()))
    assertThat(auditList[0].recallId, equalTo(recallId))
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
  fun `get assessedByUserId (UUID value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(assessedByUserId = authenticatedClient.userId))

    val auditList = authenticatedClient.auditForField(recallId, "assessedByUserId")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(authenticatedClient.userId.toString()))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get sentenceYears (nested Integer value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        sentenceLength = Api.SentenceLength(3, 0, 0),
        sentenceDate = LocalDate.now(),
        licenceExpiryDate = LocalDate.now(),
        indexOffence = "Offence 1",
        sentencingCourt = CourtId("ACCRYC"),
        sentenceExpiryDate = LocalDate.now()
      )
    )

    val auditList = authenticatedClient.auditForField(recallId, "sentencingInfo.sentenceLength.sentenceYears")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(3))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get sentenceExpiryDate (nested LocalDate value) audit history`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val updatedRecall = authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        sentenceLength = Api.SentenceLength(3, 0, 0),
        sentenceDate = LocalDate.now(),
        licenceExpiryDate = LocalDate.now(),
        indexOffence = "Offence 1",
        sentencingCourt = CourtId("ACCRYC"),
        sentenceExpiryDate = LocalDate.now()
      )
    )

    val auditList = authenticatedClient.auditForField(recallId, "sentencingInfo.sentenceExpiryDate")

    assertThat(auditList.size, equalTo(1))
    assertThat(auditList[0].updatedValue, equalTo(LocalDate.now().toString()))
    assertThat(auditList[0].recallId, equalTo(recallId))
    assertThat(auditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertOffsetDateTimesEqual(auditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  @Test
  fun `get audit summary for a recall`() {
    val savedRecall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Brian"), null, LastName("Badgering")))
    val recallId = savedRecall.recallId

    val auditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(auditList.map { it.fieldName.value }, equalTo(listOf("lastUpdatedByUserId", "licenceNameCategory", "lastUpdatedDateTime", "nomsNumber", "createdByUserId", "createdDateTime", "id", "firstName", "lastName")))

    assertThat(auditList[2].fieldName, equalTo(FieldName("lastUpdatedDateTime")))
    assertThat(auditList[2].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(auditList[2].auditCount, equalTo(1))
    assertOffsetDateTimesEqual(auditList[2].updatedDateTime, savedRecall.lastUpdatedDateTime)

    val updatedRecall = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(contraband = true))

    val updatedAuditList = authenticatedClient.auditSummaryForRecall(recallId)

    assertThat(updatedAuditList.map { it.fieldName.value }, equalTo(listOf("contraband", "lastUpdatedByUserId", "licenceNameCategory", "lastUpdatedDateTime", "nomsNumber", "createdByUserId", "createdDateTime", "id", "recallType", "firstName", "lastName")))

    assertThat(updatedAuditList[0].fieldName, equalTo(FieldName("contraband")))
    assertThat(updatedAuditList[0].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(updatedAuditList[0].auditCount, equalTo(1))
    assertOffsetDateTimesEqual(updatedAuditList[0].updatedDateTime, updatedRecall.lastUpdatedDateTime)

    assertThat(updatedAuditList[3].fieldName, equalTo(FieldName("lastUpdatedDateTime")))
    assertThat(updatedAuditList[3].updatedByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(updatedAuditList[3].auditCount, equalTo(2))
    assertOffsetDateTimesEqual(updatedAuditList[3].updatedDateTime, updatedRecall.lastUpdatedDateTime)
  }

  // Due to differences in precision we need to allow some variance on OffsetDateTimes
  fun assertOffsetDateTimesEqual(expected: OffsetDateTime, actual: OffsetDateTime): AbstractOffsetDateTimeAssert<*> =
    assertThat(expected).isCloseTo(actual, within(1, ChronoUnit.MILLIS))!!
}
