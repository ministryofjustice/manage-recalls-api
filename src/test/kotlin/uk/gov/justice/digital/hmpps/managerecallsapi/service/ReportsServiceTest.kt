package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomBookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReportsService.Api.ReportCategory.WEEKLY_RECALLS_NEW
import java.io.StringReader
import java.time.OffsetDateTime

class ReportsServiceTest {
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = ReportsService(
    recallRepository,
  )

  private val now = OffsetDateTime.now()

  private val headers = arrayOf(
    "CUSTODY_TYPE_DESCRIPTION",
    "CUSTODY_TYPE_AT_TIME_OF_RECALL_DESCRIPTION",
    "FAMILY_NAME",
    "FIRST_NAMES",
    "NOMS_ID",
    "PRISON_NUMBER",
  )

  @Test
  fun `getWeeklyRecallsNew returns expected response wrapping zero recalls`() {

    every { recallRepository.findAllByCreatedDateTimeIsBetweenOrderByCreatedDateTimeAsc(any(), any()) } returns emptyList()

    val response = underTest.getWeeklyRecallsNew(now.minusDays(7))

    assertThat(response.category, equalTo(WEEKLY_RECALLS_NEW))
    assertThat(response.mimeType, equalTo("text/csv"))
    assertThat(response.fileName, equalTo(FileName("weekly_recalls_new.csv")))
    assertThat(response.content, startsWith("${headers[0]},${headers[1]}"))

    val csvParser: CSVParser? = CSVFormat.DEFAULT
      .withHeader(*headers)
      .withFirstRecordAsHeader()
      .parse(StringReader(response.content))

    val headerNames = csvParser!!.headerNames
    assertThat(headerNames, equalTo(headers.asList()))
    assertThat(csvParser.records.size, equalTo(0))
  }

  @Test
  fun `getWeeklyRecallsNew returns expected response wrapping a single recall`() {
    val recall = mockk<Recall>()
    val nomsNumber = randomNoms()
    val bookingNumber = randomBookingNumber()
    every { recall.inCustodyAtBooking } returns false
    every { recall.inCustodyAtAssessment } returns true
    every { recall.prisonerName() } returns PersonName("Firstname", "Billy Bob", "Surname")
    every { recall.nomsNumber } returns nomsNumber
    every { recall.bookingNumber } returns bookingNumber

    val recalls = listOf(recall)

    every { recallRepository.findAllByCreatedDateTimeIsBetweenOrderByCreatedDateTimeAsc(any(), any()) } returns recalls

    val response = underTest.getWeeklyRecallsNew(now.minusDays(7))

    assertThat(response.category, equalTo(WEEKLY_RECALLS_NEW))
    assertThat(response.mimeType, equalTo("text/csv"))
    assertThat(response.fileName, equalTo(FileName("weekly_recalls_new.csv")))
    assertThat(response.content, startsWith("${headers[0]},${headers[1]}"))

    val csvParser: CSVParser? = CSVFormat.DEFAULT
      .withHeader(*headers)
      .withFirstRecordAsHeader()
      .parse(StringReader(response.content))

    val headerNames = csvParser!!.headerNames
    assertThat(headerNames, equalTo(headers.asList()))

    val rowOne = csvParser.records.get(0)
    assertThat(rowOne.get(0), equalTo("Not in custody"))
    assertThat(rowOne.get(1), equalTo("In custody"))
    assertThat(rowOne.get(2), equalTo("Surname"))
    assertThat(rowOne.get(3), equalTo("Firstname Billy Bob"))

    assertThat(rowOne.get("CUSTODY_TYPE_DESCRIPTION"), equalTo("Not in custody"))
    assertThat(rowOne.get("CUSTODY_TYPE_AT_TIME_OF_RECALL_DESCRIPTION"), equalTo("In custody"))
    assertThat(rowOne.get("FAMILY_NAME"), equalTo("Surname"))
    assertThat(rowOne.get("FIRST_NAMES"), equalTo("Firstname Billy Bob"))
    assertThat(rowOne.get("NOMS_ID"), equalTo(nomsNumber.value))
    assertThat(rowOne.get("PRISON_NUMBER"), equalTo(bookingNumber.value))
  }
}
