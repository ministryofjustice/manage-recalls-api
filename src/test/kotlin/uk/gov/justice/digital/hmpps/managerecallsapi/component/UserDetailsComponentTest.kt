package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class UserDetailsComponentTest : ComponentTestBase() {

  @Test
  fun `can save user details`() {
    val userId = ::UserId.random()
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val response = authenticatedClient.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName, signature, email, phoneNumber))

    assertThat(response, equalTo(UserDetailsResponse(userId, firstName, lastName, signature, email, phoneNumber)))
  }

  @Test
  fun `save user details with blank userId returns 400`() {
    authenticatedClient.post("/users", getJson(userId = ""))
      .expectStatus().isBadRequest
  }

  private fun getJson(
    userId: String = ::UserId.random().toString(),
    firstName: String = "firstName",
    lastName: String = "lastName",
    signature: String = "signature",
    email: String = "email@domain.com",
    phoneNumber: String = "01234567890",
  ) = "{\"userId\":\"$userId\",\"firstName\": \"$firstName\",\"lastName\": \"$lastName\",\"signature\": \"$signature\", \"email\" : \"$email\", \"phoneNumber\": \"$phoneNumber\"}"

  @Test
  fun `save user details with blank firstName returns 400`() {
    authenticatedClient.post("/users", getJson(firstName = ""))
      .expectStatus().isBadRequest
  }

  @Test
  fun `save user details with blank lastName returns 400`() {
    authenticatedClient.post("/users", getJson(lastName = ""))
      .expectStatus().isBadRequest
  }

  @Test
  fun `can get saved user details`() {
    val userId = ::UserId.random()
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    val email = Email("ppud.user@ppudreplacement.com")
    val phoneNumber = PhoneNumber("01234567890")
    authenticatedClient.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName, signature, email, phoneNumber))

    val response = authenticatedClient.getUserDetails(userId)
    assertThat(response, equalTo(UserDetailsResponse(userId, firstName, lastName, signature, email, phoneNumber)))
  }
}
