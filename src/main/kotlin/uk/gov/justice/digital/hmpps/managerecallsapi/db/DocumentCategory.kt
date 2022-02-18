package uk.gov.justice.digital.hmpps.managerecallsapi.db

enum class DocumentCategory(val uploaded: Boolean, val versioned: Boolean) {
  PART_A_RECALL_REPORT(true, true),
  LICENCE(true, true),
  OASYS_RISK_ASSESSMENT(true, true),
  PREVIOUS_CONVICTIONS_SHEET(true, true),
  PRE_SENTENCING_REPORT(true, true),
  CHARGE_SHEET(true, true),
  CPS_PAPERS(true, true),
  POLICE_REPORT(true, true),
  EXCLUSION_ZONE_MAP(true, true),
  RECALL_REQUEST_EMAIL(true, true),
  RECALL_NOTIFICATION_EMAIL(true, true),
  DOSSIER_EMAIL(true, true),
  RECALL_NOTIFICATION(false, true),
  REVOCATION_ORDER(false, true),
  LETTER_TO_PRISON(false, true),
  DOSSIER(false, true),
  REASONS_FOR_RECALL(false, true),
  MISSING_DOCUMENTS_EMAIL(true, true),
  RESCIND_REQUEST_EMAIL(true, false),
  RESCIND_DECISION_EMAIL(true, false),
  NOTE_DOCUMENT(true, false),
  OTHER(true, false),
  UNCATEGORISED(true, false)
}
