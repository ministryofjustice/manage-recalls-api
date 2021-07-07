package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException

class ManageRecallsApiExceptionHandlerTest {

  private val underTest = ManageRecallsApiExceptionHandler()
  private val objectName = "objectName"

  @Test
  fun `handles MethodArgumentNotValidException with no binding errors`() {
    val result = underTest.handleException(
      methodArgumentNotValidExceptionWithNoBindingErrors()
    )

    assertThat(
      result,
      isResponseEntityMatching(
        BAD_REQUEST,
        ErrorResponse(BAD_REQUEST, "")
      )
    )
  }

  @Test
  fun `handles MethodArgumentNotValidException with field errors`() {
    val fieldError1 = fieldError("fieldName1", "field error message1")
    val fieldError2 = fieldError("fieldName2", "field error message2")

    val result = underTest.handleException(
      methodArgumentNotValidExceptionWithFieldErrors(fieldError1, fieldError2)
    )

    assertThat(
      result,
      isResponseEntityMatching(
        BAD_REQUEST,
        ErrorResponse(BAD_REQUEST, "fieldName1: field error message1, fieldName2: field error message2")
      )
    )
  }

  @Test
  fun `handles MethodArgumentNotValidException with object errors`() {
    val objectError1 = objectError("object error message1")
    val objectError2 = objectError("object error message2")

    val result = underTest.handleException(
      methodArgumentNotValidExceptionWithFieldErrors(objectError1, objectError2)
    )

    assertThat(
      result,
      isResponseEntityMatching(
        BAD_REQUEST,
        ErrorResponse(BAD_REQUEST, "object error message1, object error message2")
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

  private fun objectError(defaultMessage: String) =
    ObjectError(objectName, defaultMessage)

  private fun fieldError(fieldName: String, defaultMessage: String) =
    FieldError(objectName, fieldName, defaultMessage)

  private fun methodArgumentNotValidExceptionWithNoBindingErrors(): MethodArgumentNotValidException =
    methodArgumentNotValidExceptionWithFieldErrors()

  private fun methodArgumentNotValidExceptionWithFieldErrors(vararg errors: ObjectError): MethodArgumentNotValidException {
    val bindingResult: BindingResult = BeanPropertyBindingResult(object {}, objectName)
    for (error in errors) {
      bindingResult.addError(error)
    }
    return MethodArgumentNotValidException(mockk(relaxed = true), bindingResult)
  }
}
