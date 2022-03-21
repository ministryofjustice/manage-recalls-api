package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient.BankHoliday
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient.BankHolidayResponse
import java.time.LocalDate

class BankHolidayRegisterClientTest {

  @Test
  fun `check caching on multiple calls`() {
    val webClient = mockk<TimeoutHandlingWebClient>()

    val expected = BankHolidayResponse("unused", listOf(BankHoliday(LocalDate.now())))
    every { webClient.getWithTimeout<BankHolidayResponse>(any(), any(), any()) } returns Mono.just(expected)

    val underTest = BankHolidayRegisterClient(webClient)

    for (i in 1..3) {
      underTest.getBankHolidays().block()
    }

    verify(exactly = 1) { webClient.getWithTimeout<BankHolidayResponse>(any(), any(), any()) }
  }
}
