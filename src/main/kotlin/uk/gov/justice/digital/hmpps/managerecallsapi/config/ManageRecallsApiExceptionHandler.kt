package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException

@RestControllerAdvice
class ManageRecallsApiExceptionHandler {
  private val log = LoggerFactory.getLogger(this::class.java)

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
    with(e.message) {
      log.info("HttpMessageNotReadableException {}", this)
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(BAD_REQUEST, this))
    }

  @ExceptionHandler(NotFoundException::class)
  fun handleException(e: NotFoundException): ResponseEntity<ErrorResponse> =
    with(e) {
      log.info(e.toString())
      ResponseEntity
        .status(NOT_FOUND)
        .body(ErrorResponse(NOT_FOUND, e.toString()))
    }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected error: ${e.message}"))
  }

  @ExceptionHandler(ResponseStatusException::class)
  fun handleException(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
    log.error("ResponseStatusException {}", e)
    return ResponseEntity
      .status(e.status)
      .body(ErrorResponse(e.status, e.message))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected AccessDeniedException", e)
    return ResponseEntity
      .status(UNAUTHORIZED)
      .body(ErrorResponse(UNAUTHORIZED, "Access Denied: ${e.message}"))
  }
}

data class ErrorResponse(val status: HttpStatus, val message: String?)
