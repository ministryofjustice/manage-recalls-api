package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court

class CourtValidationServiceTest {

  private val courtRegister = mockk<CourtRegisterClient>()
  private val underTest = CourtValidationService(courtRegister)

  @Test
  fun `check court is in register`() {
    val courtId = CourtId("MWI")
    every { courtRegister.findById(courtId) } returns Mono.just(Court(courtId, CourtName("MWI name")))

    val courtValidationResult = underTest.isValid(courtId)

    assertThat(courtValidationResult, equalTo(true))
  }

  @Test
  fun `return true if court id is null`() {
    val courtValidationResult = underTest.isValid(null)

    assertThat(courtValidationResult, equalTo(true))
  }
}
