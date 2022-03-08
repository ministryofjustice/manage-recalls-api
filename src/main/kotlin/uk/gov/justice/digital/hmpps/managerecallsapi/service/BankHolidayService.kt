package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class BankHolidayService(@Autowired private val bankHolidayClient: BankHolidayRegisterClient) {
  private fun isHoliday(date: LocalDate): Boolean {
    return bankHolidayClient.getBankHolidays().block()!!.any {
      it.date == date
    }
  }

  fun nextWorkingDate(sourceDate: LocalDate): LocalDate {
    var nextDate = sourceDate.plusDays(1)
    while (
      nextDate.dayOfWeek == DayOfWeek.SATURDAY ||
      nextDate.dayOfWeek == DayOfWeek.SUNDAY ||
      isHoliday(nextDate)
    ) {
      nextDate = nextDate.plusDays(1)
    }
    return nextDate
  }

  fun plusWorkingDays(localDate: LocalDate, numberOfDays: Int): LocalDate {
    return if (numberOfDays == 0)
      localDate
    else
      plusWorkingDays(nextWorkingDate(localDate), numberOfDays - 1)
  }
}
