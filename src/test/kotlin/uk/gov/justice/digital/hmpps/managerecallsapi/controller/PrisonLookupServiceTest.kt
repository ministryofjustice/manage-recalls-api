package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedInstance

class PrisonLookupServiceTest {
  private val prisonRegisterClient = mockk<PrisonRegisterClient>()

  private val underTest = PrisonLookupService(prisonRegisterClient)

  private val prisonId = PrisonId("prisonId")

  @Test
  fun `get prison name from prison register`() {
    val prisonName = PrisonName("prisonName")

    every { prisonRegisterClient.getAllPrisons() } returns Mono.just(
      listOf(
        Prison(prisonId, prisonName), fullyPopulatedInstance(), fullyPopulatedInstance(), fullyPopulatedInstance()
      )
    )

    val result = underTest.getPrisonName(prisonId)

    assertThat(result, equalTo(prisonName))
  }

  @Test
  fun `throws exception if no prison found for the supplied prisonId`() {
    every { prisonRegisterClient.getAllPrisons() } returns Mono.just(
      listOf(
        fullyPopulatedInstance(), fullyPopulatedInstance(), fullyPopulatedInstance()
      )
    )

    assertThrows<PrisonNotFoundException> { underTest.getPrisonName(prisonId) }
  }
}
