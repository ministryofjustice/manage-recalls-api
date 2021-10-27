package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate

@Component
class BankHolidayRegisterClient {

  @Autowired
  internal lateinit var bankHolidayRegisterWebClient: WebClient

  fun getBankHolidays(): Mono<List<BankHoliday>> {
    return bankHolidayRegisterWebClient
      .get()
      .uri("/england-and-wales.json")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<BankHolidayResponse>() {}).map { it.events }
  }

  data class BankHolidayResponse(
    val division: String,
    val events: List<BankHoliday>
  )

  data class BankHoliday(
    val date: LocalDate
  )
}
