package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus.CREATED
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ApprovalTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [UserController::class])
class UserControllerTest(@Autowired private val userController: UserController) : ApprovalTestCase() {
  @Test
  fun `can add user details`(approver: ContentApprover) {
    approver(CREATED) {
      userController.addUserDetails(AddUserDetailsRequest(::UserId.zeroes(), FirstName("Jimmy"), LastName("Ppud")))
    }
  }
}
