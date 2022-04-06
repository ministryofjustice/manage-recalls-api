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
  fun `delete all recalls before testing reports`() {
    `delete all recalls`()
  }

  val headers = arrayOf(
    "CUSTODY_TYPE_DESCRIPTION",
    "CUSTODY_TYPE_AT_TIME_OF_RECALL_DESCRIPTION",
    "FAMILY_NAME",
    "FIRST_NAMES"
  )

  private val recallRequest = BookRecallRequest(
    randomNoms(),
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

    val records: List<CSVRecord> = CSVFormat.DEFAULT
      .withHeader(*headers)
      .withFirstRecordAsHeader()
      .parse(StringReader(response.content))
      .iterator().asSequence().toList()

    assertThat(records.size, equalTo(1))
    assertThat(records[0].get(0), equalTo("Not set"))
    assertThat(records[0].get(1), equalTo("Not set"))
    assertThat(records[0].get(2), equalTo("Badgerette"))
    assertThat(records[0].get(3), equalTo("Marianne"))
  }
}
