package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient

class PrisonValidationServiceTest {

  private val prisonRegister = mockk<PrisonRegisterClient>()
  private val underTest = PrisonValidationService(prisonRegister)

  @Test
  fun `can check prison is in register`() {
    val prisonList: MutableList<Prison> = ArrayList()
    prisonList.add(Prison(PrisonId("HPI"), PrisonName("HPI name"), false))
    prisonList.add(Prison(PrisonId("MWI"), PrisonName("MWI name"), true))
    prisonList.add(Prison(PrisonId("ALI"), PrisonName("ALI name"), true))
    prisonList.add(Prison(PrisonId("FYI"), PrisonName("FYI name"), false))

    every { prisonRegister.getAllPrisons() } returns Mono.just(prisonList)

    val prisonValidationResult = underTest.isPrisonValidAndActive(PrisonId("MWI"))

    assertThat(prisonValidationResult, equalTo(true))
  }

  @Test
  fun `can check prison is not in register`() {
    val prisonList: MutableList<Prison> = ArrayList()
    prisonList.add(Prison(PrisonId("HPI"), PrisonName("MWI Name"), true))
    prisonList.add(Prison(PrisonId("ALI"), PrisonName("ALI Name"), true))
    prisonList.add(Prison(PrisonId("FYI"), PrisonName("FYI Name"), true))

    every { prisonRegister.getAllPrisons() } returns Mono.just(prisonList)

    val prisonValidationResult = underTest.isPrisonValidAndActive(PrisonId("MWI"))

    assertThat(prisonValidationResult, equalTo(false))
  }

  @Test
  fun `can return true if prison name is null`() {

    val prisonValidationResult = underTest.isPrisonValidAndActive(null)

    assertThat(prisonValidationResult, equalTo(true))
  }
}
