package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus.CREATED
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ApprovalTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.io.File
import java.util.Base64

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [UserController::class])
class UserControllerTest(@Autowired private val userController: UserController) : ApprovalTestCase() {

  @MockkBean private lateinit var userDetailsService: UserDetailsService

  @Test
  fun `can add user details`(approver: ContentApprover) {
    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val signature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val userDetails = UserDetails(userId, firstName, lastName, signature, email, phoneNumber)

    every { userDetailsService.save(userDetails) } returns userDetails

    approver(CREATED) {
      userController.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName, signature, email, phoneNumber))
    }
  }

  @Test
  fun `can get user details`() {
    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val signature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())
    val userDetails = UserDetails(userId, firstName, lastName, signature, email, phoneNumber)

    every { userDetailsService.get(userId) } returns userDetails

    val result = userController.getUserDetails(userId)
    assertThat(result, equalTo(UserDetailsResponse(userId, firstName, lastName, signature, email, phoneNumber)))
  }
}
