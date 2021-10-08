package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient

@Service
class CourtValidationService(@Autowired private val courtRegisterClient: CourtRegisterClient) {

  fun isValid(courtId: CourtId?): Boolean {
    return courtId?.let {
      courtRegisterClient.getAllCourts().block()?.any { (it.courtId == courtId) } ?: false
    } ?: true
  }
}
