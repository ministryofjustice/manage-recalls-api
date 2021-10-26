package uk.gov.justice.digital.hmpps.managerecallsapi.db

enum class RecallDocumentCategory(val versioned: Boolean) {
  PART_A_RECALL_REPORT(true),
  LICENCE(true),
  OASYS_RISK_ASSESSMENT(true),
  PREVIOUS_CONVICTIONS_SHEET(true),
  PRE_SENTENCING_REPORT(true),
  CHARGE_SHEET(true),
  CPS_PAPERS(true),
  POLICE_REPORT(true),
  EXCLUSION_ZONE_MAP(true),
  RECALL_REQUEST_EMAIL(true),
  RECALL_NOTIFICATION_EMAIL(true),
  DOSSIER_EMAIL(true),
  RECALL_NOTIFICATION(true),
  REVOCATION_ORDER(true),
  LETTER_TO_PRISON(true),
  OTHER(false),
  UNCATEGORISED(false)
}
