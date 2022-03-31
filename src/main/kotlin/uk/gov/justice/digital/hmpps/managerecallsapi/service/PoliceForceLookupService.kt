package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.PoliceForceNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PoliceUkApiClient

@Service
class PoliceForceLookupService(@Autowired private val policeUkApiClient: PoliceUkApiClient) {

  fun getPoliceForceName(policeForceId: PoliceForceId): PoliceForceName =
    policeUkApiClient.findById(policeForceId)
      .mapNotNull { it?.name }
      .block() ?: throw PoliceForceNotFoundException(policeForceId)
}
