package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.micrometer.core.instrument.Counter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate

@Component
class BankHolidayRegisterClient(
  @Autowired internal val bankHolidayRegisterWebClient: WebClient,
  @Value("\${clientApi.timeout}") val timeout: Long,
  @Autowired private val bankHolidayRegisterTimeoutCounter: Counter,
) : ErrorHandlingClient(bankHolidayRegisterWebClient, timeout, bankHolidayRegisterTimeoutCounter) {
  // Note: no mock implementation: real is used also for all tests; likely responses will be cached in future anyway

  fun getBankHolidays(): Mono<List<BankHoliday>> =
    getResponse("", object : ParameterizedTypeReference<BankHolidayResponse>() {})
      .map { it.events }

  data class BankHolidayResponse(
    val division: String,
    val events: List<BankHoliday>
  )

  data class BankHoliday(
    val date: LocalDate
  )
}
