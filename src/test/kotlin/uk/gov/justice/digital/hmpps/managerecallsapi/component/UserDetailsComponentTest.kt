package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber

class UserDetailsComponentTest : ComponentTestBase() {

  @Test
  fun `can save user details`() {
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val response = authenticatedClient.addUserDetails(
      AddUserDetailsRequest(firstName, lastName, signature, email, phoneNumber, CaseworkerBand.FOUR_PLUS),
      UserDetailsResponse::class.java
    )

    assertThat(
      response,
      equalTo(
        UserDetailsResponse(
          authenticatedClient.userId,
          firstName,
          lastName,
          signature,
          email,
          phoneNumber,
          CaseworkerBand.FOUR_PLUS
        )
      )
    )
  }

  @Test
  fun `can save user details when signature mime-type is jpeg`() {
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpeg")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val response = authenticatedClient.addUserDetails(
      AddUserDetailsRequest(firstName, lastName, signature, email, phoneNumber, CaseworkerBand.FOUR_PLUS),
      UserDetailsResponse::class.java
    )

    assertThat(
      response,
      equalTo(
        UserDetailsResponse(
          authenticatedClient.userId,
          firstName,
          lastName,
          signature,
          email,
          phoneNumber,
          CaseworkerBand.FOUR_PLUS
        )
      )
    )
  }

  @Test
  fun `cant save user details when signature mime-type is not jpg`() {
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/document/3_pages_unnumbered.pdf")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")

    val response = authenticatedClient.addUserDetails(
      AddUserDetailsRequest(firstName, lastName, signature, email, phoneNumber, CaseworkerBand.FOUR_PLUS),
      ErrorResponse::class.java,
      HttpStatus.BAD_REQUEST
    )

    assertThat(response, equalTo(ErrorResponse(HttpStatus.BAD_REQUEST, "UnsupportedFileTypeException: pdf")))
  }

  private fun getJson(
    firstName: String = "firstName",
    lastName: String = "lastName",
    signature: String = "signature",
    email: String = "email@domain.com",
    phoneNumber: String = "01234567890",
  ) =
    "{\"firstName\": \"$firstName\",\"lastName\": \"$lastName\",\"signature\": \"$signature\", \"email\" : \"$email\", \"phoneNumber\": \"$phoneNumber\"}"

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
  fun `can get saved current user details`() {
    val firstName = FirstName("PPUD")
    val lastName = LastName("USER")
    val signature = base64EncodedFileContents("/signature.jpg")
    val email = Email("ppud.user@ppudreplacement.com")
    val phoneNumber = PhoneNumber("01234567890")
    authenticatedClient.addUserDetails(
      AddUserDetailsRequest(
        firstName,
        lastName,
        signature,
        email,
        phoneNumber,
        CaseworkerBand.FOUR_PLUS
      ),
      UserDetailsResponse::class.java
    )

    val response = authenticatedClient.getCurrentUserDetails()
    assertThat(
      response,
      equalTo(
        UserDetailsResponse(
          authenticatedClient.userId,
          firstName,
          lastName,
          signature,
          email,
          phoneNumber,
          CaseworkerBand.FOUR_PLUS
        )
      )
    )
  }
}
