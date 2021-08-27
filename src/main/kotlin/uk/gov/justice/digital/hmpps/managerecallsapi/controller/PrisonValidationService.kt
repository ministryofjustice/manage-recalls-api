package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.stereotype.Service

@Service
class PrisonValidationService {

  fun isValidPrison(prison: String?): Boolean = true
}
