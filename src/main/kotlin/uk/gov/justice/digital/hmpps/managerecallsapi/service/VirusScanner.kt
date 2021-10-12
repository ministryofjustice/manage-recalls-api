package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import xyz.capybara.clamav.ClamavClient
import xyz.capybara.clamav.commands.scan.result.ScanResult.OK
import xyz.capybara.clamav.commands.scan.result.ScanResult.VirusFound

@Component
class VirusScanner(@Autowired private val clamavClient: ClamavClient) {
  fun scan(documentBytes: ByteArray): VirusScanResult =
    when (val scanResult = clamavClient.scan(documentBytes.inputStream())) {
      OK -> NoVirusFound
      is VirusFound -> VirusScanResult.VirusFound(scanResult.foundViruses)
    }
}

sealed class VirusScanResult {
  object NoVirusFound : VirusScanResult()
  data class VirusFound(val foundViruses: Map<String, Collection<String>>) : VirusScanResult()
}
