package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

@Service
class PrisonLookupService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {

  // TODO: Make this not nullable
  fun getPrisonName(prisonId: String?): String? {
    prisonId.let {
      // TODO Are we going to remove calls to block() ... seems to trigger e,g, knock on issues with chaining
      return prisonRegisterClient.getAllPrisons().block()?.find { it.prisonId == prisonId }?.prisonName
    }
  }
}
