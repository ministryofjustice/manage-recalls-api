package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient

@Service
class CourtLookupService(@Autowired private val courtRegisterClient: CourtRegisterClient) {
  fun getCourtName(courtId: CourtId): CourtName =
    courtRegisterClient.findById(courtId)
      .mapNotNull { it?.courtName }
      .block() ?: throw CourtNotFoundException(courtId)
}

data class CourtNotFoundException(val courtId: CourtId) : Exception()
