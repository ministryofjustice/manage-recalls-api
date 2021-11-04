package uk.gov.justice.digital.hmpps.managerecallsapi.service

import net.sf.jmimemagic.Magic
import org.postgresql.util.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId

@Service
class UserDetailsService(
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  private val validMimeTypes = setOf("jpg")

  fun save(userDetails: UserDetails): UserDetails =
    forValidDocumentMimeType(Base64.decode(userDetails.signature)) {
      return userDetailsRepository.save(userDetails)
    }

  fun get(userId: UserId): UserDetails = userDetailsRepository.getByUserId(userId)
  fun find(userId: UserId): UserDetails? = userDetailsRepository.findByUserId(userId)

  private inline fun <reified T : Any?> forValidDocumentMimeType(bytes: ByteArray, fn: () -> T): T {
    val magicMatch = Magic.getMagicMatch(bytes)
    if (!validMimeTypes.contains(magicMatch.extension))
      throw UnsupportedFileTypeException(magicMatch.extension)
    return fn()
  }
}
class UnsupportedFileTypeException(override val message: String?) : ManageRecallsException(message)
