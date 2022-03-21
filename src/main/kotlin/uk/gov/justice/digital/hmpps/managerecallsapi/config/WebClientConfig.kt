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
import uk.gov.justice.digital.hmpps.managerecallsapi.register.TimeoutHandlingWebClient
import java.net.URI
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfig(
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

  @Value("\${bankHolidayRegister.endpoint.url}")
  private lateinit var bankHolidayRegisterEndpointUrl: String

  @Bean("bankHolidayRegisterWebClient")
  fun bankHolidayRegisterWebClient(): TimeoutHandlingWebClient = timeoutHandlingWebClient(bankHolidayRegisterEndpointUrl)

  @Value("\${courtRegister.endpoint.url}")
  private lateinit var courtRegisterEndpointUrl: String

  @Bean("courtRegisterWebClient")
  fun courtRegisterWebClient(): TimeoutHandlingWebClient = timeoutHandlingWebClient(courtRegisterEndpointUrl)

  @Value("\${gotenberg.endpoint.url}")
  private lateinit var gotenbergEndpointUrl: String

  @Bean("gotenbergWebClient")
  fun gotenbergWebClient(): WebClient = webClient(gotenbergEndpointUrl)

  @Value("\${policeUkApi.endpoint.url}")
  private lateinit var policeUkApiEndpointUrl: String

  @Bean("policeUkApiWebClient")
  fun policeUkApiWebClient(): TimeoutHandlingWebClient = timeoutHandlingWebClient(policeUkApiEndpointUrl)

  @Value("\${prisonRegister.endpoint.url}")
  private lateinit var prisonRegisterEndpointUrl: String

  @Bean("prisonRegisterWebClient")
  fun prisonRegisterWebClient(): TimeoutHandlingWebClient = timeoutHandlingWebClient(prisonRegisterEndpointUrl)

  private fun webClient(endpointUrl: String): WebClient {
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
    webClient(endpointUrl),
    timeout,
    timeoutCounter(endpointUrl)
  )
}
