package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient.Court

class CourtValidationServiceTest {

  private val courtRegister = mockk<CourtRegisterClient>()
  private val underTest = CourtValidationService(courtRegister)

  @Test
  fun `check court is in register`() {
    val courtList: MutableList<Court> = ArrayList()
    courtList.add(Court(CourtId("HPI"), CourtName("HPI name")))
    courtList.add(Court(CourtId("MWI"), CourtName("MWI name")))
    courtList.add(Court(CourtId("ALI"), CourtName("ALI name")))
    courtList.add(Court(CourtId("FYI"), CourtName("FYI name")))

    every { courtRegister.getAllCourts() } returns Mono.just(courtList)

    val courtValidationResult = underTest.isValid(CourtId("MWI"))

    assertThat(courtValidationResult, equalTo(true))
  }

  @Test
  fun `return true if court id is null`() {
    val courtValidationResult = underTest.isValid(null)

    assertThat(courtValidationResult, equalTo(true))
  }
}
