package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

@Service
class PrisonLookupService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {
  fun getPrisonName(prisonId: String): String =
    prisonRegisterClient.getAllPrisons().mapNotNull { prisons ->
      prisons.find { it.prisonId == prisonId }?.prisonName
    }.block() ?: throw PrisonNotFoundException(prisonId)
}

data class PrisonNotFoundException(val prisonId: String) : Exception()
