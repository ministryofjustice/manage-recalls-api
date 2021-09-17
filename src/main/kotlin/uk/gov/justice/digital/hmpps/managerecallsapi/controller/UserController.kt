package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.util.UUID

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UserController {

  @PostMapping("/users")
  fun addUserDetails(@RequestBody addUserDetailsRequest: AddUserDetailsRequest) =
    ResponseEntity(
      AddUserDetailsResponse(UserId(UUID(0, 0)), FirstName("Jimmy"), LastName("Ppud")),
      HttpStatus.CREATED
    )
}

data class AddUserDetailsRequest(val userId: UserId, val firstName: FirstName, val lastName: LastName)
data class AddUserDetailsResponse(val userId: UserId, val firstName: FirstName, val lastName: LastName)
