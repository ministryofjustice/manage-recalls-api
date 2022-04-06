package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReportsService.Api.ReportCategory.WEEKLY_RECALLS_NEW
import java.time.OffsetDateTime

@Service
class ReportsService(
  @Autowired private val recallRepository: RecallRepository,
) {

  val csvMimeType = "text/csv"
  private val recallsReportCsvHeaders = arrayOf(
    "CUSTODY_TYPE_DESCRIPTION",
    "CUSTODY_TYPE_AT_TIME_OF_RECALL_DESCRIPTION",
    "FAMILY_NAME",
    "FIRST_NAMES",
    "NOMS_ID",
    "PRISON_NUMBER",
/*
GENDER - n/a today; load in future?
LICENCE_REVOKE_DATE - not stored on recall; save in future?
NOMS_REGION_DESCRIPTION - ??
OUT_OF_HOURS
PRISON_NUMBER
PROBATION_AREA_DESCRIPTION
RECALL_REASON_DESCRIPTIONS
RECALL_TYPE_DESCRIPTION
RESCIND_FLAG
 */
  )

  fun getWeeklyRecallsNew(start: OffsetDateTime): Api.GetReportResponse {
    val end = start.plusDays(7)
    val data = recallRepository.findAllByCreatedDateTimeIsBetweenOrderByCreatedDateTimeAsc(start, end)
    val out = recallsReportCsv(data)
    return Api.GetReportResponse(
      WEEKLY_RECALLS_NEW,
      csvMimeType,
      FileName("weekly_recalls_new.csv"),
      out.toString()
    )
  }

  private fun recallsReportCsv(data: List<Recall>): StringBuilder {
    val out = StringBuilder()
    val format = CSVFormat.Builder.create().setHeader(*recallsReportCsvHeaders).build()
    CSVPrinter(
      out, format
    ).use { printer ->
      data.forEach { r ->
        printer.printRecord(recallCsvRecord(r))
      }
    }
    return out
  }

  private fun recallCsvRecord(r: Recall): MutableIterable<String> =
    mutableListOf(
      custodyType(r.inCustodyAtBooking),
      custodyType(r.inCustodyAtAssessment),
      r.prisonerName().lastName.toString(),
      r.prisonerName().firstAndMiddleNames(),
      r.nomsNumber.value,
      if (r.bookingNumber == null) "Not set" else r.bookingNumber.value,
    )

  private fun custodyType(inCustodyBoolean: Boolean?): String =
    when (inCustodyBoolean) {
      true -> { "In custody" }
      false -> { "Not in custody" }
      null -> { "Not set" }
    }

  class Api {
    data class GetReportResponse(
      val category: ReportCategory,
      val mimeType: String,
      val fileName: FileName,
      val content: String,
    )

    enum class ReportCategory {
      WEEKLY_RECALLS_NEW
    }
  }
}
