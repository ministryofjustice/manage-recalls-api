package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import io.micrometer.core.instrument.Counter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson.mapper
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.TestWebClientConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.TimeoutHandlingWebClient

@ExtendWith(SpringExtension::class)
@Import(MetricsAutoConfiguration::class, CompositeMeterRegistryAutoConfiguration::class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(
  properties = ["prisonRegister.endpoint.url=http://localhost:9094"],
  classes = [TestWebClientConfig::class]
)
class PrisonRegisterIntegrationTest(
  @Autowired private val prisonRegisterTestWebClient: WebClient
) {
  private val prisonRegisterTimeoutCounter: Counter = mockk()

  private val prisonRegisterClient: PrisonRegisterClient =
    PrisonRegisterClient(TimeoutHandlingWebClient(prisonRegisterTestWebClient, 1, prisonRegisterTimeoutCounter))

  private val prisonRegisterMockServer = PrisonRegisterMockServer(mapper)

  @BeforeAll
  fun startMockServer() {
    prisonRegisterMockServer.start()
  }

  @AfterAll
  fun stopMockServer() {
    prisonRegisterMockServer.stop()
  }

  @BeforeEach
  fun resetMocks() {
    prisonRegisterMockServer.resetAll()
    prisonRegisterClient.clearCache()
  }

  @Test
  fun `can retrieve all prisons`() {
    prisonRegisterMockServer.stubPrisons()

    val result = prisonRegisterClient.getAllPrisons().block()!!

    assertThat(result, hasSize(equalTo(7)))
  }

  private fun prisons(): List<Prison> = prisonRegisterMockServer.prisons

  @ParameterizedTest(name = "find prison {0}")
  @MethodSource("prisons")
  fun `can find prison by id`(prison: Prison) {
    prisonRegisterMockServer.stubPrisons()

    val result = prisonRegisterClient.findPrisonById(prison.prisonId).block()
    assertThat(result, present(equalTo(prison)))
  }

  @Test
  fun `get prison returns empty if prison does not exist`() {
    prisonRegisterMockServer.stubPrisons()

    val result = prisonRegisterClient.findPrisonById(PrisonId("XXX")).block()

    assertThat(result, absent())
  }

  @Test
  fun `handle timeout from client`() {
    every { prisonRegisterTimeoutCounter.increment() } just Runs

    prisonRegisterMockServer.delaySearch("/prisons", 3000)

    val exception = assertThrows<RuntimeException> {
      prisonRegisterClient.getAllPrisons().block()
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientTimeoutException::class.java))
    assertThat(exception.cause!!.message, equalTo("PrisonRegisterClient: [java.util.concurrent.TimeoutException]"))

    verify(exactly = 1) { prisonRegisterTimeoutCounter.increment() }
  }

  @Test
  fun `handle exception from client`() {
    prisonRegisterMockServer.stubCallWithException("/prisons")

    val exception = assertThrows<RuntimeException> {
      prisonRegisterClient.getAllPrisons().block()!!
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientException::class.java))
    assertThat(exception.cause!!.message, equalTo("PrisonRegisterClient: [200 OK from GET http://localhost:9094/prisons; nested exception is reactor.netty.http.client.PrematureCloseException: Connection prematurely closed DURING response]"))
  }
}
