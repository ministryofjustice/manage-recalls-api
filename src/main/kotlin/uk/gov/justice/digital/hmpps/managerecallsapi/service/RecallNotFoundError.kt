package uk.gov.justice.digital.hmpps.managerecallsapi.service

import javax.persistence.EntityNotFoundException

class RecallNotFoundError(message: String, e: EntityNotFoundException) : Throwable(message, e)
