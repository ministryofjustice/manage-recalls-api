package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.Clock
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UserController(
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val tokenExtractor: TokenExtractor,
  @Autowired private val clock: Clock
) {

  @PostMapping("/users")
  fun addUserDetails(
    @RequestBody addUserDetailsRequest: AddUserDetailsRequest,
    @RequestHeader("Authorization") bearerToken: String
  ) =
    ResponseEntity(
      userDetailsService.save(
        addUserDetailsRequest.toUserDetails(
          tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
        )
      ).toResponse(),
      HttpStatus.CREATED
    )

  @GetMapping("/users/{userId}")
  fun getUserDetails(
    @PathVariable("userId") userId: UserId
  ) = userDetailsService.get(userId).toResponse()

  @GetMapping("/users/current")
  fun getCurrentUserDetails(
    @RequestHeader("Authorization") bearerToken: String
  ) = userDetailsService.get(tokenExtractor.getTokenFromHeader(bearerToken).userUuid()).toResponse()

  fun AddUserDetailsRequest.toUserDetails(userId: UserId) =
    UserDetails(
      userId,
      firstName,
      lastName,
      signature,
      email,
      phoneNumber,
      caseworkerBand,
      OffsetDateTime.now(clock)
    )
}

fun UserDetails.toResponse() =
  UserDetailsResponse(
    this.userId(),
    this.firstName,
    this.lastName,
    this.signature,
    this.email,
    this.phoneNumber,
    this.caseworkerBand
  )

data class AddUserDetailsRequest(
  val firstName: FirstName,
  val lastName: LastName,
  val signature: String,
  val email: Email,
  val phoneNumber: PhoneNumber,
  val caseworkerBand: CaseworkerBand = CaseworkerBand.FOUR_PLUS // TODO remove defaulting once UI change released
)

data class UserDetailsResponse(
  val userId: UserId,
  val firstName: FirstName,
  val lastName: LastName,
  val signature: String,
  val email: Email,
  val phoneNumber: PhoneNumber,
  val caseworkerBand: CaseworkerBand
)
