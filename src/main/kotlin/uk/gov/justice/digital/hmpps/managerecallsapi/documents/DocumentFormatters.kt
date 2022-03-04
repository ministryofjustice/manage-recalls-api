package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_1
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_2
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale.ENGLISH

const val RECALL_TEAM_NAME = "Recall Team"
const val RECALL_TEAM_CONTACT_NUMBER = "N/K"

fun String.toBase64DecodedByteArray(): ByteArray = Base64.getDecoder().decode(this)
fun ByteArray.encodeToBase64String(): String = Base64.getEncoder().encodeToString(this)
fun String.encodeToBase64String(): String = this.toByteArray().encodeToBase64String()

val standardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", ENGLISH)
val standardTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class PersonName(val firstName: FirstName, val middleNames: MiddleNames? = null, val lastName: LastName) {
  constructor(firstName: String, middleNames: String? = null, lastName: String) : this(
    FirstName(firstName),
    middleNames = middleNames?.let { MiddleNames(it) },
    lastName = LastName(lastName)
  )
  override fun toString(): String = "$firstName $lastName"

  fun firstAndLastName(): FullName = FullName(toString())

  fun firstMiddleLast(): FullName = FullName("${firstAndMiddleNames()} $lastName")

  private fun firstAndMiddleNames(): String =
    when (middleNames) {
      null -> "$firstName"
      else -> "$firstName $middleNames"
    }
}

data class RecallDescription(val recallType: RecallType, val recallLength: RecallLength?) {
  fun asFixedTermLengthDescription(): String {
    return when (recallLength!!) {
      FOURTEEN_DAYS -> "14 DAY FIXED TERM RECALL"
      TWENTY_EIGHT_DAYS -> "28 DAY FIXED TERM RECALL"
    }
  }
  fun numberOfDays(): Int {
    return when (recallLength!!) {
      FOURTEEN_DAYS -> 14
      TWENTY_EIGHT_DAYS -> 28
    }
  }
  fun tableOfContentsDescription() =
    when (recallType) {
      STANDARD -> "Standard 255c recall review"
      RecallType.FIXED -> when (recallLength!!) {
        FOURTEEN_DAYS -> "14 Day FTR under 12 months"
        TWENTY_EIGHT_DAYS -> "28 Day FTR 12 months & over"
      }
    }
  fun letterToPrisonAppealsPapersHeading() =
    when (recallLength!!) {
      FOURTEEN_DAYS -> "Annex H – Appeal Papers"
      TWENTY_EIGHT_DAYS -> "Annex I – Appeal Papers"
    }
}

fun MappaLevel.shouldShowOnDocuments(): Boolean {
  return when (this) {
    LEVEL_1, LEVEL_2, LEVEL_3 -> true
    else -> false
  }
}

class YesOrNo(val value: Boolean) {
  override fun toString(): String = if (value) "YES" else "NO"
}

class ValueOrNone(val value: String?) {
  override fun toString(): String = value ?: "None"
}

fun LocalDate.asStandardDateFormat(): String = this.format(standardDateFormatter)
fun ZonedDateTime.asStandardTimeFormat(): String = this.format(standardTimeFormatter)
