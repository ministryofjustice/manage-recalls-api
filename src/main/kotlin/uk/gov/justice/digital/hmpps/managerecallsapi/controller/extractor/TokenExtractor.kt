package uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.util.Base64
import java.util.UUID

@Component
class TokenExtractor(
  @Autowired private val decoder: Base64.Decoder
) {

  @Serializable
  data class Token(@SerialName("user_uuid") val userUuid: String) {
    fun userUuid(): UserId = UserId(UUID.fromString(userUuid))
  }

  fun getTokenFromHeader(bearerToken: String): Token {
    val chunks: List<String> = bearerToken.replace("Bearer ", "").split(".")
    val payload = String(decoder.decode(chunks[1]))
    return Json { ignoreUnknownKeys = true }.decodeFromString(payload)
  }
}
