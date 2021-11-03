package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("bankHoliday")
class BankHolidayHealth(
  webClient: WebClient,
  @Value("\${bankHolidayRegister.endpoint.url}") endpointUrl: String
) : PingHealthCheck(webClient, endpointUrl)