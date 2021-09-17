package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository

@Service
class UserDetailsService(
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  fun save(userDetails: UserDetails): UserDetails = userDetailsRepository.save(userDetails)
}
