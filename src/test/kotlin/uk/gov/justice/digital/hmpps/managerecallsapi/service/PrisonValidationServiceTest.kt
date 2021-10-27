package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PrisonRegisterClient
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class PrisonValidationServiceTest {

  private val prisonRegister = mockk<PrisonRegisterClient>()
  private val underTest = PrisonValidationService(prisonRegister)

  private val prisonId = PrisonId("AAA")
  private val activePrison = Prison(prisonId, PrisonName("Active Prison"), true)
  private val inactivePrison = Prison(prisonId, PrisonName("Inactive Prison"), false)

  private fun isValidAndActiveInputs(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(activePrison, true),
      Arguments.of(inactivePrison, false),
      Arguments.of(null, true)
    )
  }

  @ParameterizedTest(name = "{0} prison is valid and active {1}")
  @MethodSource("isValidAndActiveInputs")
  fun `isValidAndActive should return true if prison is found and active`(
    foundPrison: Prison?,
    expectedResult: Boolean
  ) {
    val prisonRegisterResult: Mono<Prison> = foundPrison?.let { Mono.just(foundPrison) } ?: Mono.empty()
    every { prisonRegister.findPrisonById(prisonId) } returns prisonRegisterResult

    val prisonValidationResult = underTest.isValidAndActive(prisonId)

    assertThat(prisonValidationResult, equalTo(expectedResult))
  }

  private fun isValidInputs(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(activePrison, true),
      Arguments.of(inactivePrison, true),
      Arguments.of(null, true)
    )
  }

  @ParameterizedTest(name = "{0} prison is found {1}")
  @MethodSource("isValidInputs")
  fun `isValidInputs should return true if prison is found`(
    foundPrison: Prison?,
    expectedResult: Boolean
  ) {
    val prisonRegisterResult: Mono<Prison> = foundPrison?.let { Mono.just(foundPrison) } ?: Mono.empty()
    every { prisonRegister.findPrisonById(prisonId) } returns prisonRegisterResult

    val prisonValidationResult = underTest.isValid(prisonId)

    assertThat(prisonValidationResult, equalTo(expectedResult))
  }
}
