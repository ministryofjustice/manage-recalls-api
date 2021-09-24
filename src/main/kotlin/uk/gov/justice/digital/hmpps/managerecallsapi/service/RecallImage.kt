package uk.gov.justice.digital.hmpps.managerecallsapi.service

enum class RecallImage(val fileName: String) {
  RevocationOrderLogo("revocation-order-logo.png"),
  RecallSummaryLogo("recall-summary-logo.png"),
  LetterToProbationLogo("letter-to-probation-logo.png");

  val path: String = "/templates/images/$fileName"
}
