package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.NO
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.YES_WITHOUT_DETAILS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.YES_WITH_DETAILS

enum class DocumentCategory(val uploaded: Boolean, val versioned: Versioned) {
  // VERSIONED WITH DETAILS
  CHARGE_SHEET(true, YES_WITH_DETAILS),
  CPS_PAPERS(true, YES_WITH_DETAILS),
  DOSSIER(false, YES_WITH_DETAILS),
  EXCLUSION_ZONE_MAP(true, YES_WITH_DETAILS),
  LETTER_TO_PRISON(false, YES_WITH_DETAILS),
  LETTER_TO_PROBATION(false, YES_WITH_DETAILS),
  LICENCE(true, YES_WITH_DETAILS),
  OASYS_RISK_ASSESSMENT(true, YES_WITH_DETAILS),
  PART_A_RECALL_REPORT(true, YES_WITH_DETAILS),
  PART_B_RISK_REPORT(true, YES_WITH_DETAILS),
  POLICE_REPORT(true, YES_WITH_DETAILS),
  PREVIOUS_CONVICTIONS_SHEET(true, YES_WITH_DETAILS),
  PRE_SENTENCING_REPORT(true, YES_WITH_DETAILS),
  REASONS_FOR_RECALL(false, YES_WITH_DETAILS),
  RECALL_NOTIFICATION(false, YES_WITH_DETAILS),
  REVOCATION_ORDER(false, YES_WITH_DETAILS),

  // VERSIONED WITHOUT DETAILS - linked to recall
  CHANGE_RECALL_TYPE_EMAIL(true, YES_WITHOUT_DETAILS),
  DOSSIER_EMAIL(true, YES_WITHOUT_DETAILS),
  MISSING_DOCUMENTS_EMAIL(true, YES_WITHOUT_DETAILS), // linked to missing doc record (make UNVERSIONED)
  NSY_REMOVE_WARRANT_EMAIL(true, YES_WITHOUT_DETAILS),
  RECALL_NOTIFICATION_EMAIL(true, YES_WITHOUT_DETAILS),
  RECALL_REQUEST_EMAIL(true, YES_WITHOUT_DETAILS),

  // UNVERSIONED document categories
  RESCIND_REQUEST_EMAIL(true, NO), // linked to rescind record
  RESCIND_DECISION_EMAIL(true, NO), // linked to rescind record
  PART_B_EMAIL_FROM_PROBATION(true, NO), // linked to part B record
  NOTE_DOCUMENT(true, NO), // linked to note record
  OTHER(true, NO),
  UNCATEGORISED(true, NO);

  fun versioned() =
    versioned == YES_WITH_DETAILS || versioned == YES_WITHOUT_DETAILS
}

enum class Versioned {
  NO,
  YES_WITH_DETAILS, // These categories require `details` for every version except version 1
  YES_WITHOUT_DETAILS // These categories do not require `details` for any version
}
