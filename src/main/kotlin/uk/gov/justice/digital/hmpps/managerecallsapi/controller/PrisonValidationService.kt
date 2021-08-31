package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

@Service
class PrisonValidationService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {

  fun isValidPrison(prisonId: String?): Boolean {
    if (prisonId == null) return true
    return prisonRegisterClient.getAllPrisons().block()?.any { it.prisonId == prisonId } ?: false
  }
}
