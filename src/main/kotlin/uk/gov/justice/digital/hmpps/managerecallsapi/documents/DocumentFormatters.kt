package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_1
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_2
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

const val RECALL_TEAM_NAME = "Recall Team"
const val RECALL_TEAM_CONTACT_NUMBER = "N/K"

val standardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", ENGLISH)
val standardTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class PersonName(val firstName: FirstName, val middleNames: MiddleNames? = null, val lastName: LastName) {
  override fun toString(): String =
    when (middleNames) {
      null -> "$firstName $lastName"
      else -> "$firstName $middleNames $lastName"
    }
}

data class FirstAndMiddleNames(val firstName: FirstName, val middleNames: MiddleNames? = null) {
  override fun toString(): String =
    when (middleNames) {
      null -> "$firstName"
      else -> "$firstName $middleNames"
    }
}

data class RecallLengthDescription(val recallLength: RecallLength) {
  fun asFixedTermLengthDescription(): String {
    return when (recallLength) {
      FOURTEEN_DAYS -> "14 DAY FIXED TERM RECALL"
      TWENTY_EIGHT_DAYS -> "28 DAY FIXED TERM RECALL"
    }
  }
  fun numberOfDays(): Int {
    return when (recallLength) {
      FOURTEEN_DAYS -> 14
      TWENTY_EIGHT_DAYS -> 28
    }
  }
  fun tableOfContentsFixedTermLengthDescription() =
    when (recallLength) {
      FOURTEEN_DAYS -> "14 Day FTR under 12 months"
      TWENTY_EIGHT_DAYS -> "28 Day FTR 12 months & over"
    }
}

fun MappaLevel.shouldShowOnDocuments(): Boolean {
  return when (this) {
    LEVEL_1, LEVEL_2, LEVEL_3 -> true
    else -> false
  }
}

fun UserDetails.fullName(): String = PersonName(this.firstName, null, this.lastName).toString()
fun Prisoner.fullName(): PersonName =
  PersonName(FirstName(this.firstName!!), this.middleNames?.let { MiddleNames(it) }, LastName(this.lastName!!))

class YesOrNo(val value: Boolean) {
  override fun toString(): String = if (value) "YES" else "NO"
}

class ValueOrNone(val value: String?) {
  override fun toString(): String = value ?: "None"
}

fun LocalDate.asStandardDateFormat(): String = this.format(standardDateFormatter)
fun ZonedDateTime.asStandardTimeFormat(): String = this.format(standardTimeFormatter)
