package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId

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

  @ExceptionHandler(MultiFileException::class)
  fun handleException(e: MultiFileException): ResponseEntity<MultiErrorResponse> {
    log.error("MultiFileException: ${e.message}: " + e.failures.map { it.first }.joinToString(", ", "for categories: "))
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(MultiErrorResponse(BAD_REQUEST, e.failures.map { FileError(it.first, it.second, e.message) }))
  }

  @ExceptionHandler(IllegalStateException::class)
  fun handleException(e: IllegalStateException): ResponseEntity<ErrorResponse> {
    log.error("IllegalStateException", e)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(ErrorResponse(FORBIDDEN, e.toString()))
  }
}

data class ErrorResponse(val status: HttpStatus, val message: String?)

data class FileError(val category: DocumentCategory, val fileName: FileName, val error: String?)

data class MultiErrorResponse(val status: HttpStatus, val fileErrors: List<FileError>)

open class ManageRecallsException(override val message: String? = null, override val cause: Throwable? = null) : Exception(message, cause) {
  override fun toString(): String {
    return if (this.message == null) {
      this.javaClass.simpleName
    } else {
      "${this.javaClass.simpleName}: ${this.message}"
    }
  }
}

open class NotFoundException : ManageRecallsException()

class ClientTimeoutException(clientName: String, errorType: String) : ManageRecallsException("$clientName: [$errorType]")
class ClientException(clientName: String, exception: Exception) : ManageRecallsException("$clientName: [${exception.message}]", exception)

class WrongDocumentTypeException(val category: DocumentCategory) : ManageRecallsException(category.name)
class MissingDetailsException(val category: DocumentCategory, val version: Int) : ManageRecallsException("$category version: [$version]")
class ReturnedToCustodyRecallExpectedException(val recallId: RecallId) : ManageRecallsException(recallId.toString())
class InvalidPrisonOrCourtException(validAndActiveCurrentPrison: Boolean, validLastReleasePrison: Boolean, validSentencingCourt: Boolean) :
  ManageRecallsException("validAndActiveCurrentPrison=[$validAndActiveCurrentPrison], validLastReleasePrison=[$validLastReleasePrison], validSentencingCourt=[$validSentencingCourt]")

data class WrongPhaseStartException(val recallId: RecallId, val phase: Phase) : ManageRecallsException("$recallId: [$phase]")
data class MissingPhaseStartException(val recallId: RecallId, val phase: Phase) : ManageRecallsException("$recallId: [$phase]")

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class DocumentNotFoundException(val recallId: RecallId, val documentId: DocumentId) : NotFoundException()
data class LastKnownAddressNotFoundException(val recallId: RecallId, val lastKnownAddressId: LastKnownAddressId) : NotFoundException()
data class RescindRecordNotFoundException(val recallId: RecallId, val rescindRecordId: RescindRecordId) : NotFoundException()
data class RescindRecordAlreadyDecidedException(val recallId: RecallId, val rescindRecordId: RescindRecordId) : java.lang.IllegalStateException()
data class UndecidedRescindRecordAlreadyExistsException(val recallId: RecallId) : java.lang.IllegalStateException()
data class InvalidStopReasonException(val recallId: RecallId, val stopReason: StopReason) : java.lang.IllegalStateException()
data class RecallDocumentWithCategoryNotFoundException(val recallId: RecallId, val documentCategory: DocumentCategory) : NotFoundException()

class VirusFoundException : ManageRecallsException()
class MultiFileException(override val message: String, val failures: List<Pair<DocumentCategory, FileName>>) : Exception()
class DocumentDeleteException(override val message: String?) : ManageRecallsException(message)
class IllegalDocumentStateException(override val message: String?) : ManageRecallsException(message)

data class CourtNotFoundException(val courtId: CourtId) : NotFoundException()
data class PrisonNotFoundException(val prisonId: PrisonId) : NotFoundException()
data class PoliceForceNotFoundException(val policeForceId: PoliceForceId) : NotFoundException()
data class PrisonerNotFoundException(val nomsNumber: NomsNumber) : NotFoundException()
