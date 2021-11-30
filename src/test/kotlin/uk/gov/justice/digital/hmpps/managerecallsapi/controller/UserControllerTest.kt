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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [UserController::class])
class UserControllerTest(@Autowired private val userController: UserController) : ApprovalTestCase() {

  @MockkBean private lateinit var userDetailsService: UserDetailsService
  @MockkBean private lateinit var tokenExtractor: TokenExtractor
  @MockkBean private lateinit var fixedClock: Clock

  @Test
  fun `can add user details`(approver: ContentApprover) {
    every { fixedClock.instant() } returns Instant.parse("2021-10-04T13:15:50.00Z")
    every { fixedClock.zone } returns ZoneId.of("UTC")

    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val signature = File("src/test/resources/signature.jpg").readBytes().encodeToBase64String()
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val userDetails = UserDetails(
      userId,
      firstName,
      lastName,
      signature,
      email,
      phoneNumber,
      CaseworkerBand.THREE,
      OffsetDateTime.now(fixedClock)
    )

    every { tokenExtractor.getTokenFromHeader(any()) } returns TokenExtractor.Token(userId.toString())
    every { userDetailsService.save(userDetails) } returns userDetails

    approver(CREATED) {
      userController.addUserDetails(
        AddUserDetailsRequest(
          firstName,
          lastName,
          signature,
          email,
          phoneNumber,
          CaseworkerBand.THREE
        ),
        "Bearer Token"
      )
    }
  }

  @Test
  fun `can get user details`() {
    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val signature = File("src/test/resources/signature.jpg").readBytes().encodeToBase64String()
    val userDetails = UserDetails(
      userId,
      firstName,
      lastName,
      signature,
      email,
      phoneNumber,
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

    every { userDetailsService.get(userId) } returns userDetails

    val result = userController.getUserDetails(userId)
    assertThat(
      result,
      equalTo(
        UserDetailsResponse(
          userId,
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
  fun `can get current user details`() {
    val userId = ::UserId.zeroes()
    val firstName = FirstName("Jimmy")
    val lastName = LastName("Ppud")
    val email = Email("bertie@badger.org")
    val phoneNumber = PhoneNumber("01234567890")
    val signature = File("src/test/resources/signature.jpg").readBytes().encodeToBase64String()
    val userDetails = UserDetails(
      userId,
      firstName,
      lastName,
      signature,
      email,
      phoneNumber,
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

    every { tokenExtractor.getTokenFromHeader(any()) } returns TokenExtractor.Token(userId.toString())
    every { userDetailsService.get(userId) } returns userDetails

    val result = userController.getCurrentUserDetails("Bearer Token")
    assertThat(
      result,
      equalTo(
        UserDetailsResponse(
          userId,
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
