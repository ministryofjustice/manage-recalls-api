package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class UserDetailsComponentTest : ComponentTestBase() {

  @Test
  fun `can save user details`() {
    val userId = ::UserId.random()
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    val response = authenticatedClient.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName, signature))

    assertThat(response, equalTo(UserDetailsResponse(userId, firstName, lastName, signature)))
  }

  @Test
  fun `save user details with blank userId returns 400`() {
    authenticatedClient.post("/users", "{\"userId\":\"\",\"firstName\": \"firstName\",\"lastName\": \"lastName\"}")
      .expectStatus().isBadRequest
  }

  @Test
  fun `save user details with blank firstName returns 400`() {
    authenticatedClient.post("/users", "{\"userId\":\"${::UserId.random()}\",\"firstName\": \"\",\"lastName\": \"lastName\"}")
      .expectStatus().isBadRequest
  }

  @Test
  fun `save user details with blank lastName returns 400`() {
    authenticatedClient.post("/users", "{\"userId\":\"${::UserId.random()}\",\"firstName\": \"firstName\",\"lastName\": \"\"}")
      .expectStatus().isBadRequest
  }

  @Test
  fun `can get saved user details`() {
    val userId = ::UserId.random()
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    authenticatedClient.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName, signature))

    val response = authenticatedClient.getUserDetails(userId)
    assertThat(response, equalTo(UserDetailsResponse(userId, firstName, lastName, signature)))
  }
}
