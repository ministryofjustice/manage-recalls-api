package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient

@Service
class CourtValidationService(@Autowired private val courtRegisterClient: CourtRegisterClient) {

  fun isValid(courtId: CourtId?): Boolean {
    return courtId?.let {
      courtRegisterClient.findById(courtId).mapNotNull { true }.block()
    } ?: true
  }
}
