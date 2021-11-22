package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PoliceUkApiClient

@Service
class PoliceForceValidationService(@Autowired private val policeUkApiClient: PoliceUkApiClient) {

  fun isValid(policeForceId: PoliceForceId?): Boolean {
    return policeForceId?.let {
      policeUkApiClient.findById(policeForceId).mapNotNull { true }.block()
    } ?: true
  }
}
