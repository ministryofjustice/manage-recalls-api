package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchController
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomAdultDateOfBirth
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class SearchComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  // TODO PUD-1578
  // @Test
  // fun `returns 404 if get prisoner returns not found`() {
  //   prisonerOffenderSearchMockServer.getPrisonerByNomsNumberReturnsWith(nomsNumber, NOT_FOUND)
  //
  //   val result = authenticatedClient.get("/prisoner/$nomsNumber", NOT_FOUND)
  //     .expectBody(ErrorResponse::class.java).returnResult().responseBody!!
  //
  //   assertThat(
  //     result,
  //     equalTo(
  //       ErrorResponse(
  //         NOT_FOUND,
  //         "ClientException: PrisonerOffenderSearchClient: [401 Unauthorized from GET http://localhost:9092/prisoner/123456]"
  //       )
  //     )
  //   )
  // }

  @Test
  fun `returns 500 if get prisoner returns unauthorized`() {
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberReturnsWith(nomsNumber, UNAUTHORIZED)

    val result = authenticatedClient.get("/prisoner/$nomsNumber", INTERNAL_SERVER_ERROR)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(
      result,
      equalTo(
        ErrorResponse(
          INTERNAL_SERVER_ERROR,
          "ClientException: PrisonerOffenderSearchClient: [401 Unauthorized from GET http://localhost:9092/prisoner/123456]"
        )
      )
    )
  }

  @Test
  fun `can send get request to PrisonerOffenderSearch api and return response`() {
    val prisoner = testPrisoner(nomsNumber)
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(nomsNumber, prisoner)

    val response = authenticatedClient.prisonerByNomsNumber(nomsNumber)

    assertThat(response, equalTo(prisoner.apiPrisoner()))
  }

  @Test
  fun `PrisonerOffenderSearch timeout is handled gracefully`() {
    val nomsNumber = randomNoms()
    prisonerOffenderSearchMockServer.delayGetPrisoner(nomsNumber, 1500)

    val result = authenticatedClient.prisonerByNomsNumber(nomsNumber, GATEWAY_TIMEOUT)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(
      result,
      equalTo(ErrorResponse(GATEWAY_TIMEOUT, "PrisonerOffenderSearchClient: [java.util.concurrent.TimeoutException]"))
    )
  }

  private fun Prisoner.apiPrisoner(): SearchController.Api.Prisoner =
    SearchController.Api.Prisoner(
      firstName,
      middleNames,
      lastName,
      dateOfBirth,
      gender,
      prisonerNumber,
      pncNumber,
      croNumber,
    )

  private fun testPrisoner(nomsNumber: NomsNumber?) = Prisoner(
    prisonerNumber = nomsNumber?.value,
    pncNumber = randomAlphanumeric(1, 32),
    croNumber = randomAlphanumeric(1, 32),
    firstName = randomAlphanumeric(1, 32),
    middleNames = randomAlphanumeric(1, 32),
    lastName = randomAlphanumeric(1, 32),
    dateOfBirth = randomAdultDateOfBirth(),
    gender = randomAlphanumeric(1, 32),
  )
}
