package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.PrisonRegisterClient

@Service
class PrisonLookupService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {
  fun getPrisonName(prisonId: PrisonId): PrisonName =
    prisonRegisterClient.findPrisonById(prisonId)
      .mapNotNull { it?.prisonName }
      .block() ?: throw PrisonNotFoundException(prisonId)
}

data class PrisonNotFoundException(val prisonId: PrisonId) : Exception()
