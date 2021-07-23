package uk.gov.justice.digital.hmpps.managerecallsapi.service

class RecallNotFoundError(message: String, e: Throwable) : Throwable(message, e)
