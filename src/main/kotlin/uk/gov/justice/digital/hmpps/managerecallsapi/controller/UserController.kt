package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UserController(
  @Autowired private val userDetailsService: UserDetailsService,
) {

  @PostMapping("/users")
  fun addUserDetails(@RequestBody addUserDetailsRequest: AddUserDetailsRequest) =
    ResponseEntity(
      userDetailsService.save(addUserDetailsRequest.toUserDetails()).toResponse(),
      HttpStatus.CREATED
    )
}

fun AddUserDetailsRequest.toUserDetails() = UserDetails(userId, firstName, lastName)
fun UserDetails.toResponse() = UserDetailsResponse(this.userId(), this.firstName, this.lastName)

data class AddUserDetailsRequest(val userId: UserId, val firstName: FirstName, val lastName: LastName)
data class UserDetailsResponse(val userId: UserId, val firstName: FirstName, val lastName: LastName)
