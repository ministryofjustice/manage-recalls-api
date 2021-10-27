package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.register.BankHolidayRegisterClient
import java.time.LocalDate

@Component
class BankHolidayService(@Autowired private val bankHolidayClient: BankHolidayRegisterClient) {
  fun isHoliday(date: LocalDate): Boolean {
    return bankHolidayClient.getBankHolidays().block()!!.any {
      it.date == date
    }
  }
}
