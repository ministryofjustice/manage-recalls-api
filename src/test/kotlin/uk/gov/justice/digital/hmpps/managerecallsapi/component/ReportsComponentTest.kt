package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReportsService.Api.ReportCategory.WEEKLY_RECALLS_NEW
import java.io.StringReader

class ReportsComponentTest : ComponentTestBase() {

  @BeforeEach
  fun `delete all recall data before testing reports`() {
    `delete all recall data`()
  }

  val headers = arrayOf(
    "CUSTODY_TYPE_DESCRIPTION",
    "CUSTODY_TYPE_AT_TIME_OF_RECALL_DESCRIPTION",
    "FAMILY_NAME",
    "FIRST_NAMES",
    "NOMS_ID",
    "PRISON_NUMBER",
  )

  final val nomsNumber = randomNoms()

  private val recallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Marianne"),
    null,
    LastName("Badgerette"),
    CroNumber("3454/56A"),
    randomHistoricalDate()
  )

  @Test
  fun `weeklyRecallsNew returns wrapped csv of the two recalls just created`() {
    authenticatedClient.bookRecall(recallRequest)

    val response = authenticatedClient.weeklyRecallsNew()
    assertThat(response.category, equalTo(WEEKLY_RECALLS_NEW))
    assertThat(response.mimeType, equalTo("text/csv"))
    assertThat(response.fileName, equalTo(FileName("weekly_recalls_new.csv")))
    assertThat(response.content, startsWith("${headers[0]},${headers[1]}"))

    val records: List<CSVRecord> = CSVFormat.Builder.create()
      .setHeader(*headers)
      .setSkipHeaderRecord(false)
      .build()
      .parse(StringReader(response.content))
      .iterator().asSequence().toList()

    assertThat(records.size, equalTo(2))
    val headerRecord = records[0]
    assertThat(headerRecord.get(0), equalTo(headers[0]))
    assertThat(headerRecord.get(1), equalTo(headers[1]))
    assertThat(headerRecord.get(2), equalTo(headers[2]))
    assertThat(headerRecord.get(3), equalTo(headers[3]))
    assertThat(headerRecord.get(4), equalTo(headers[4]))
    assertThat(headerRecord.get(5), equalTo(headers[5]))
    val firstDataRecord = records[1]
    assertThat(firstDataRecord.get(0), equalTo("Not set"))
    assertThat(firstDataRecord.get(1), equalTo("Not set"))
    assertThat(firstDataRecord.get(2), equalTo("Badgerette"))
    assertThat(firstDataRecord.get(3), equalTo("Marianne"))
    assertThat(firstDataRecord.get(4), equalTo(nomsNumber.value))
    assertThat(firstDataRecord.get(5), equalTo("Not set"))
  }
}
