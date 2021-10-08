package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.PrisonRegisterClient

class PrisonLookupServiceTest {
  private val prisonRegisterClient = mockk<PrisonRegisterClient>()

  private val underTest = PrisonLookupService(prisonRegisterClient)

  private val prisonId = PrisonId("AAA")
  private val prisonName = PrisonName("Active Prison")
  private val prison = Prison(prisonId, prisonName, true)

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
}
