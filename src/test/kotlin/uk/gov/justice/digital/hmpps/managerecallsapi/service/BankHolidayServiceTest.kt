package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient.BankHoliday
import java.time.LocalDate

@TestInstance(PER_CLASS)
class BankHolidayServiceTest {
  private val bankHolidayRegisterClient = mockk<BankHolidayRegisterClient>()
  private val underTest = BankHolidayService(bankHolidayRegisterClient)

  @Test
  fun `nextWorkingDate when in custody and recallNotificationEmailSentDateTime is on Wednesday should be Thursday`() {
    every { bankHolidayRegisterClient.getBankHolidays() } returns Mono.just(emptyList())
    val nextWorkingDate = underTest.nextWorkingDate(LocalDate.of(2021, 10, 6))

    assertThat(nextWorkingDate, equalTo(LocalDate.of(2021, 10, 7)))
  }

  @Test
  fun `nextWorkingDate when in custody and  recallNotificationEmailSentDateTime is on Friday should be Monday`() {
    every { bankHolidayRegisterClient.getBankHolidays() } returns Mono.just(emptyList())

    val nextWorkingDate = underTest.nextWorkingDate(LocalDate.of(2021, 10, 8))

    assertThat(nextWorkingDate, equalTo(LocalDate.of(2021, 10, 11)))
  }

  @Test
  fun `nextWorkingDate when  in custody and recallNotificationEmailSentDateTime is day before weekend and bank holidays should be first non-weekend and non-bank-holiday`() {
    every { bankHolidayRegisterClient.getBankHolidays() } returns Mono.just(
      listOf(
        BankHoliday(LocalDate.of(2021, 12, 27)),
        BankHoliday(LocalDate.of(2021, 12, 28))
      )
    )
    val nextWorkingDate = underTest.nextWorkingDate(LocalDate.of(2021, 12, 24))

    assertThat(nextWorkingDate, equalTo(LocalDate.of(2021, 12, 29)))
  }

  @Test
  fun `plusWorkingDays increments appropriate number of days`() {
    every { bankHolidayRegisterClient.getBankHolidays() } returns Mono.just(emptyList())

    val date = LocalDate.of(2022, 3, 8)
    val plus10days = underTest.plusWorkingDays(date, 10)
    assertThat(plus10days, equalTo(LocalDate.of(2022, 3, 22)))
  }
}
