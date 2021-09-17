package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class SaveUserDetailsComponentTest : ComponentTestBase() {

  @Test
  fun `can save user details`() {
    val userId = ::UserId.random()
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val response = authenticatedClient.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName))

    assertThat(response, equalTo(UserDetailsResponse(userId, firstName, lastName)))
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
}