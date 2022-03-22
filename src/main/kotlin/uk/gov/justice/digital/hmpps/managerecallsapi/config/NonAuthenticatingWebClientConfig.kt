package uk.gov.justice.digital.hmpps.managerecallsapi.config

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.TimeoutHandlingWebClient
import java.net.URI
import java.time.Duration.ofSeconds

@Configuration
class NonAuthenticatingWebClientConfig(
  @Autowired private val metricsCustomizer: MetricsWebClientCustomizer,
  @Autowired private val meterRegistry: MeterRegistry,
  @Value("\${clientApi.timeout}") val timeout: Long,
) {

  @Bean("webClientNoAuthNoMetrics") // health check only, not logging metrics
  fun webClientNoAuthNoMetrics(): WebClient =
    WebClient.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
      .clientConnector(
        ReactorClientHttpConnector(HttpClient.create().responseTimeout(ofSeconds(10)))
      )
      .build()

  @Bean("bankHolidayRegisterWebClient")
  fun bankHolidayRegisterWebClient(@Value("\${bankHolidayRegister.endpoint.url}") bankHolidayRegisterEndpointUrl: String) =
    timeoutHandlingWebClient(bankHolidayRegisterEndpointUrl)

  @Bean("courtRegisterWebClient")
  fun courtRegisterWebClient(@Value("\${courtRegister.endpoint.url}") courtRegisterEndpointUrl: String) =
    timeoutHandlingWebClient(courtRegisterEndpointUrl)

  @Bean("gotenbergWebClient")
  fun gotenbergWebClient(@Value("\${gotenberg.endpoint.url}") gotenbergEndpointUrl: String): WebClient =
    webClientWithMetricsNoAuth(gotenbergEndpointUrl)

  @Bean("policeUkApiWebClient")
  fun policeUkApiWebClient(@Value("\${policeUkApi.endpoint.url}") policeUkApiEndpointUrl: String) =
    timeoutHandlingWebClient(policeUkApiEndpointUrl)

  @Bean("prisonRegisterWebClient")
  fun prisonRegisterWebClient(@Value("\${prisonRegister.endpoint.url}") prisonRegisterEndpointUrl: String) =
    timeoutHandlingWebClient(prisonRegisterEndpointUrl)

  private fun webClientWithMetricsNoAuth(endpointUrl: String): WebClient {
    val builder = WebClient.builder()
    metricsCustomizer.customize(builder)
    return builder
      .baseUrl(endpointUrl)
      .codecs {
        it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
        it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
      }
      .build()
  }

  private fun timeoutCounter(endpointUrl: String): Counter {
    val metricName = "http_client_requests_timeout"
    val host = URI(endpointUrl).host
    return meterRegistry.counter(metricName, Tags.of("clientName", host))
  }

  private fun timeoutHandlingWebClient(endpointUrl: String) = TimeoutHandlingWebClient(
    webClientWithMetricsNoAuth(endpointUrl),
    timeout,
    timeoutCounter(endpointUrl)
  )
}
