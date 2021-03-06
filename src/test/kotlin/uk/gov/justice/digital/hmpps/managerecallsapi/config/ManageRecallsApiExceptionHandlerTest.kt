package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName

class ManageRecallsApiExceptionHandlerTest {

  private val underTest = ManageRecallsApiExceptionHandler()

  @Test
  fun `NotFoundExceptions should be translated to 400`() {
    val notFoundException = object : NotFoundException() {}
    val result = underTest.handleException(notFoundException)

    assertThat(
      result,
      isResponseEntityMatching(NOT_FOUND, ErrorResponse(NOT_FOUND, notFoundException.toString()))
    )
  }

  @Test
  fun `ResponseStatusException should be translated to the corresponding status`() {
    val exception = ResponseStatusException(BAD_REQUEST, "This is bad")
    val result = underTest.handleException(exception)

    assertThat(
      result,
      isResponseEntityMatching(BAD_REQUEST, ErrorResponse(BAD_REQUEST, exception.message))
    )
  }

  @Test
  fun `ClientTimeoutException should be translated to the corresponding status`() {
    val exception = ClientTimeoutException("Client ABC", "SomeTimeoutException")
    val result = underTest.handleException(exception)

    assertThat(
      result,
      isResponseEntityMatching(GATEWAY_TIMEOUT, ErrorResponse(GATEWAY_TIMEOUT, "Client ABC: [SomeTimeoutException]"))
    )
  }

  @Test
  fun `ClientException should be translated to the corresponding status`() {
    val exception = ClientException("Client ABC", WebClientResponseException(404, "Blah", null, null, null))
    val result = underTest.handleException(exception)

    assertThat(
      result,
      isResponseEntityMatching(INTERNAL_SERVER_ERROR, ErrorResponse(INTERNAL_SERVER_ERROR, "ClientException: Client ABC: [404 Blah]"))
    )
  }

  @Test
  fun `ManageRecallsException should be translated to 400`() {
    val exception = ManageRecallsException("message", IllegalArgumentException())
    val result = underTest.handleException(exception)

    assertThat(
      result,
      isResponseEntityMatching(BAD_REQUEST, ErrorResponse(BAD_REQUEST, exception.toString()))
    )
  }

  @Test
  fun `VirusFoundException should be translated to 400`() {
    val exception = VirusFoundException()
    val result = underTest.handleException(exception)

    assertThat(
      result,
      isResponseEntityMatching(BAD_REQUEST, ErrorResponse(BAD_REQUEST, exception.toString()))
    )
  }

  @Test
  fun `MultiFileException should be translated to 400 with list of file details and errors in body`() {
    val firstCategory = PART_B_RISK_REPORT
    val firstFileName = FileName("part B file.pdf")
    val secondCategory = OASYS_RISK_ASSESSMENT
    val secondFileName = FileName("oasys file.pdf")
    val error = "VirusFoundException"

    val exception = MultiFileException(
      "VirusFoundException",
      listOf(
        Pair(firstCategory, firstFileName),
        Pair(secondCategory, secondFileName),
      )
    )

    val expectedErrorResponse = MultiErrorResponse(
      BAD_REQUEST,
      listOf(
        FileError(firstCategory, firstFileName, error),
        FileError(secondCategory, secondFileName, error)
      )
    )

    val result = underTest.handleException(exception)

    // TODO: Verify log call.  Do we have a means to test log calls?  Perhaps with mockkStatic(UtilJava::class) or mockkObject(UtilKotlin)?

    assertThat(
      result,
      allOf(
        has("statusCode", { it.statusCode }, equalTo(BAD_REQUEST)),
        has("body", { it.body }, present(equalTo(expectedErrorResponse)))
      )
    )
  }

  private fun isResponseEntityMatching(
    expectedHttpStatus: HttpStatus,
    expectedErrorResponse: ErrorResponse
  ): Matcher<ResponseEntity<ErrorResponse>> =
    allOf(
      has("statusCode", { it.statusCode }, equalTo(expectedHttpStatus)),
      has("body", { it.body }, present(equalTo(expectedErrorResponse)))
    )
}
