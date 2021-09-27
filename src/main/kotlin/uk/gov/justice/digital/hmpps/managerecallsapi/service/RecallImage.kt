package uk.gov.justice.digital.hmpps.managerecallsapi.service

enum class RecallImage(val fileName: String) {
  RevocationOrderLogo("revocation-order-logo.png"),
  RecallSummaryLogo("hmpps-logo.png"),
  LetterToProbationLogo("hmpps-logo.png"),
  TableOfContentsLogo("hmpps-logo.png");

  val path: String = "/templates/images/$fileName"
}
