package uk.gov.justice.digital.hmpps.managerecallsapi.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthenticationHelper(
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-id}") private val apiClientId: String
) {
  private val keyPair: KeyPair

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Bean
  fun jwtDecoder(): JwtDecoder {
    return NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()
  }

  @Bean("apiClientJwt")
  fun apiClientJwt(): String = createTestJwt(
    ::UserId.random(),
    subject = apiClientId
  )

  fun createTestJwt(
    userId: UserId,
    role: String? = null,
    subject: String = "Bertie",
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString()
  ): String {
    val claims = mutableMapOf(
      "client_id" to "test_client_id",
      "username" to subject,
      "authorities" to role?.let { listOf(role) }.orEmpty(),
      "user_uuid" to userId.toString()
    )
    return Jwts.builder()
      .setId(jwtId)
      .setSubject(subject)
      .addClaims(claims)
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }
}
