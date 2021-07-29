package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import javax.persistence.EntityNotFoundException

class ManageRecallsApiExceptionHandlerTest {

  private val underTest = ManageRecallsApiExceptionHandler()

  @Test
  fun `NotFoundExceptions should be translated to 400`() {
    val errorMessage = "Recall not found: '${::RecallId.random()}'"
    val result = underTest.handleException(
      RecallNotFoundException(errorMessage, JpaObjectRetrievalFailureException(EntityNotFoundException()))
    )

    assertThat(result, isResponseEntityMatching(NOT_FOUND, ErrorResponse(NOT_FOUND, errorMessage)))
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
