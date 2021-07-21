package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.validation.ValidationException

@RestControllerAdvice
class ManageRecallsApiExceptionHandler {
  private val log = LoggerFactory.getLogger(this::class.java)

  // TODO:  Is this needed?
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(BAD_REQUEST, "Validation failure: ${e.message}"))
  }

  // TODO:  Is this needed?
  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
    with(e.errorMessage()) {
      log.info("MethodArgumentNotValidException {}", this)
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(BAD_REQUEST, this))
    }

  private fun MethodArgumentNotValidException.errorMessage(): String =
    this.bindingResult.allErrors.joinToString { error: ObjectError ->
      when (error) {
        is FieldError -> "${error.field}: ${error.defaultMessage}"
        else -> error.defaultMessage ?: "unknown"
      }
    }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
    with(e.message) {
      log.info("HttpMessageNotReadableException {}", this)
      ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(BAD_REQUEST, this))
    }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected error: ${e.message}"))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleException(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected AccessDeniedException", e)
    return ResponseEntity
      .status(UNAUTHORIZED)
      .body(ErrorResponse(UNAUTHORIZED, "Access Denied: ${e.message}"))
  }
}

data class ErrorResponse(val status: HttpStatus, val message: String?)
