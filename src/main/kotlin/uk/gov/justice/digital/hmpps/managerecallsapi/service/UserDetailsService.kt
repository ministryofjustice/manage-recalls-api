package uk.gov.justice.digital.hmpps.managerecallsapi.service

import net.sf.jmimemagic.Magic
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetailsRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.util.Base64

@Service
class UserDetailsService(
  @Autowired private val userDetailsRepository: UserDetailsRepository
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val validMimeTypes = setOf("jpg")

  private var cache: Map<UserId, UserDetails> = emptyMap()

  fun save(userDetails: UserDetails): UserDetails {
    val savedDetails = forValidDocumentMimeType(Base64.getDecoder().decode(userDetails.signature)) {
      userDetailsRepository.save(userDetails)
    }
    clearCache()
    return savedDetails
  }

  fun get(userId: UserId): UserDetails = cache[userId] ?: getUserDetails(userId)

  private fun getUserDetails(userId: UserId): UserDetails {
    log.info("Missed cache, querying repo")
    return userDetailsRepository.getByUserId(userId)
  }

  private inline fun <reified T : Any?> forValidDocumentMimeType(bytes: ByteArray, fn: () -> T): T {
    val magicMatch = Magic.getMagicMatch(bytes)
    if (!validMimeTypes.contains(magicMatch.extension))
      throw UnsupportedFileTypeException(magicMatch.extension)
    return fn()
  }

  fun cacheAllIfEmpty() {
    if (cache.isEmpty()) {
      cache = userDetailsRepository.findAll().associateBy { UserId(it.id) }
    }
  }

  fun clearCache() {
    cache = emptyMap()
  }
}

class UnsupportedFileTypeException(override val message: String?) : ManageRecallsException(message)
