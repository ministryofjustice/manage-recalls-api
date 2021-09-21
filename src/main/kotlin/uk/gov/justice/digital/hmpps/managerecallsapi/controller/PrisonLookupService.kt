package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

@Service
class PrisonLookupService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {

  fun getPrisonName(prisonId: String?): String? {
    prisonId.let {
      return prisonRegisterClient.getAllPrisons().block()?.find { it.prisonId == prisonId }?.prisonName
    }
  }
}
