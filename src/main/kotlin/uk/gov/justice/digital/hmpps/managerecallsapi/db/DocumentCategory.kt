package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.NO
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.YES_WITHOUT_DETAILS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Versioned.YES_WITH_DETAILS

enum class DocumentCategory(val uploaded: Boolean, val versioned: Versioned) {
  CHARGE_SHEET(true, YES_WITH_DETAILS),
  CPS_PAPERS(true, YES_WITH_DETAILS),
  DOSSIER(false, YES_WITH_DETAILS),
  DOSSIER_EMAIL(true, YES_WITHOUT_DETAILS),
  EXCLUSION_ZONE_MAP(true, YES_WITH_DETAILS),
  LETTER_TO_PRISON(false, YES_WITH_DETAILS),
  LICENCE(true, YES_WITH_DETAILS),
  MISSING_DOCUMENTS_EMAIL(true, YES_WITHOUT_DETAILS),
  NSY_REMOVE_WARRANT_EMAIL(true, YES_WITHOUT_DETAILS),
  OASYS_RISK_ASSESSMENT(true, YES_WITH_DETAILS),
  PART_A_RECALL_REPORT(true, YES_WITH_DETAILS),
  POLICE_REPORT(true, YES_WITH_DETAILS),
  PREVIOUS_CONVICTIONS_SHEET(true, YES_WITH_DETAILS),
  PRE_SENTENCING_REPORT(true, YES_WITH_DETAILS),
  REASONS_FOR_RECALL(false, YES_WITH_DETAILS),
  RECALL_NOTIFICATION(false, YES_WITH_DETAILS),
  RECALL_NOTIFICATION_EMAIL(true, YES_WITHOUT_DETAILS),
  RECALL_REQUEST_EMAIL(true, YES_WITHOUT_DETAILS),
  REVOCATION_ORDER(false, YES_WITH_DETAILS),
  // UNVERSIONED document categories
  RESCIND_REQUEST_EMAIL(true, NO),
  RESCIND_DECISION_EMAIL(true, NO),
  NOTE_DOCUMENT(true, NO),
  OTHER(true, NO),
  UNCATEGORISED(true, NO);

  fun versioned() =
    versioned == YES_WITH_DETAILS || versioned == YES_WITHOUT_DETAILS
}

enum class Versioned {
  NO,
  YES_WITH_DETAILS,
  YES_WITHOUT_DETAILS
}
