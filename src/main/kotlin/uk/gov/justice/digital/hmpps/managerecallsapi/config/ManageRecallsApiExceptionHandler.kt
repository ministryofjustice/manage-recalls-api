package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
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

  @ExceptionHandler(ClientTimeoutException::class)
  fun handleException(e: ClientTimeoutException): ResponseEntity<ErrorResponse> {
    log.error("ClientTimeoutException", e)
    return ResponseEntity
      .status(GATEWAY_TIMEOUT)
      .body(ErrorResponse(GATEWAY_TIMEOUT, e.message))
  }

  @ExceptionHandler(NotFoundException::class)
  fun handleException(e: NotFoundException): ResponseEntity<ErrorResponse> =
    with(e) {
      log.info(e.toString())
      ResponseEntity
        .status(NOT_FOUND)
        .body(ErrorResponse(NOT_FOUND, e.toString()))
    }

  @ExceptionHandler(ClientException::class)
  fun handleException(e: ClientException): ResponseEntity<ErrorResponse> =
    with(e) {
      log.info(e.toString())
      ResponseEntity
        .status(INTERNAL_SERVER_ERROR)
        .body(ErrorResponse(INTERNAL_SERVER_ERROR, e.toString()))
    }

  @ExceptionHandler(ManageRecallsException::class)
  fun handleException(e: ManageRecallsException): ResponseEntity<ErrorResponse> {
    log.error("ManageRecallsException", e)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(BAD_REQUEST, e.toString()))
  }
}

data class ErrorResponse(val status: HttpStatus, val message: String?)

open class ManageRecallsException(override val message: String? = null, override val cause: Throwable? = null) : Exception(message, cause) {
  override fun toString(): String {
    return if (this.message == null) {
      this.javaClass.simpleName
    } else {
      "${this.javaClass.simpleName}: ${this.message}"
    }
  }
}

class ClientTimeoutException(clientName: String, errorType: String) : ManageRecallsException("$clientName: [$errorType]")
class ClientException(clientName: String, exception: Exception) : ManageRecallsException("$clientName: [${exception.message}]", exception)

class WrongDocumentTypeException(val category: DocumentCategory) : ManageRecallsException(category.name)
