package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PrisonRegisterClient

@Service
class PrisonValidationService(@Autowired private val prisonRegisterClient: PrisonRegisterClient) {

  fun isValidAndActive(prisonId: PrisonId?): Boolean = prisonFoundWith(prisonId) { it.active }

  fun isValid(prisonId: PrisonId?): Boolean = prisonFoundWith(prisonId) { true }

  private fun prisonFoundWith(prisonId: PrisonId?, validationFn: (Api.Prison) -> Boolean): Boolean =
    prisonId?.let {
      prisonRegisterClient.findPrisonById(prisonId).mapNotNull(validationFn).block()
    } ?: true
}
