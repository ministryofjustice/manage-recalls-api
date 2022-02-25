package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("policeUkApi")
class PoliceUkApiHealth(
  webClientNoAuthNoMetrics: WebClient,
  @Value("policeUkApi") componentName: String,
  @Value("\${policeUkApi.endpoint.url}") endpointUrl: String
) : PingHealthCheck(webClientNoAuthNoMetrics, componentName, endpointUrl)
