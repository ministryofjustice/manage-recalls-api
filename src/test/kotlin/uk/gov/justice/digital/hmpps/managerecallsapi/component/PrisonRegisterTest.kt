package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

class PrisonRegisterTest : ComponentTestBase() {

  private val prisonRegister = mockk<PrisonRegisterClient>()
  private val prisonValidationService = PrisonValidationService(prisonRegister)

  @Test
  fun `can check prison is in register`() {
    val prisonList: MutableList<PrisonRegisterClient.Prison> = ArrayList()
    prisonList.add(PrisonRegisterClient.Prison("HPI"))
    prisonList.add(PrisonRegisterClient.Prison("MWI"))
    prisonList.add(PrisonRegisterClient.Prison("ALI"))
    prisonList.add(PrisonRegisterClient.Prison("FYI"))

    every { prisonRegister.getAllPrisons() } returns Mono.just(prisonList)

    val prisonValidationResult = prisonValidationService.isValidPrison("MWI")

    assertThat(prisonValidationResult, equalTo(true))
  }

  @Test
  fun `can check prison is not in register`() {
    val prisonList: MutableList<PrisonRegisterClient.Prison> = ArrayList()
    prisonList.add(PrisonRegisterClient.Prison("HPI"))
    prisonList.add(PrisonRegisterClient.Prison("ALI"))
    prisonList.add(PrisonRegisterClient.Prison("FYI"))

    every { prisonRegister.getAllPrisons() } returns Mono.just(prisonList)

    val prisonValidationResult = prisonValidationService.isValidPrison("MWI")

    assertThat(prisonValidationResult, equalTo(false))
  }

  @Test
  fun `can return true if prison name is null`() {

    val prisonValidationResult = prisonValidationService.isValidPrison(null)

    assertThat(prisonValidationResult, equalTo(true))
  }
}
