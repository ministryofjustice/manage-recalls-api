package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound

@Component
class VirusScanner {
  fun scan(documentBytes: ByteArray): VirusScanResult = NoVirusFound
}

sealed class VirusScanResult {
  object NoVirusFound : VirusScanResult()
  object VirusFound : VirusScanResult()
}
