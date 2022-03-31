package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.PrisonNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PrisonRegisterClient
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrisonLookupServiceTest {
  private val prisonRegisterClient = mockk<PrisonRegisterClient>()

  private val underTest = PrisonLookupService(prisonRegisterClient)

  private val prisonId = PrisonId("AAA")
  private val prisonName = PrisonName("Active Prison")
  private val prison = Api.Prison(prisonId, prisonName, true)

  @Test
  fun `can get prison name for a prison`() {
    every { prisonRegisterClient.findPrisonById(prisonId) } returns Mono.just(prison)

    val result = underTest.getPrisonName(prisonId)

    assertThat(result, equalTo(prisonName))
  }

  @Test
  fun `throws PrisonNotFoundException if no prison found for the supplied prisonId`() {
    every { prisonRegisterClient.findPrisonById(prisonId) } returns Mono.empty()

    assertThrows<PrisonNotFoundException> { underTest.getPrisonName(prisonId) }
  }

  @ParameterizedTest(name = "isWelsh is true for all and only prisons in Wales")
  @MethodSource("sampleOfPrisonIdsIncludingAllWelshAsTrue")
  fun `isWelsh returned as true for all prison codes identified as inWales whilst sample of others all return false`(
    prisonId: String,
    expected: Boolean
  ) {
    assertThat(underTest.isWelsh(PrisonId(prisonId)), equalTo(expected))
  }

  private fun sampleOfPrisonIdsIncludingAllWelshAsTrue(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of("ALI", false),
      Arguments.of("BWI", true),
      Arguments.of("CFI", true),
      Arguments.of("PRI", true),
      Arguments.of("SWI", true),
      Arguments.of("UKI", true),
      Arguments.of("UPI", true),
      Arguments.of("MWI", false),
    )
  }
}
