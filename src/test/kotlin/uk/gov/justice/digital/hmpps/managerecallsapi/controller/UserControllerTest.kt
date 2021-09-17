package uk.gov.justice.digital.hmpps.managerecallsapi.controller

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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [UserController::class])
class UserControllerTest(@Autowired private val userController: UserController) : ApprovalTestCase() {

  @MockkBean private lateinit var userDetailsService: UserDetailsService

  @Test
  fun `can add user details`(approver: ContentApprover) {
    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val userDetails = UserDetails(userId, firstName, lastName)

    every { userDetailsService.save(userDetails) } returns userDetails

    approver(CREATED) {
      userController.addUserDetails(AddUserDetailsRequest(userId, firstName, lastName))
    }
  }
}
