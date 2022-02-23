package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName

enum class RecallImage(val fileName: FileName) {
  RevocationOrderLogo(FileName("revocation-order-logo.png")),
  HmppsLogo(FileName("hmpps-logo.png"));

  val path: String = "/templates/images/$fileName"
}
