package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

@Service
class PrisonValidationService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {

  fun isPrisonValidAndActive(prisonId: PrisonId?): Boolean {
    if (prisonId == null) return true
    return prisonRegisterClient.getAllPrisons().block()?.any { (it.prisonId == prisonId) && (it.active == true) } ?: false
  }

  fun isPrisonValid(prisonId: PrisonId?): Boolean {
    if (prisonId == null) return true
    return prisonRegisterClient.getAllPrisons().block()?.any { (it.prisonId == prisonId) } ?: false
  }
}