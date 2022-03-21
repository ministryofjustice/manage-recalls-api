package uk.gov.justice.digital.hmpps.managerecallsapi.search

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.Counter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson.mapper
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.TestWebClientConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(SpringExtension::class)
@Import(MetricsAutoConfiguration::class, CompositeMeterRegistryAutoConfiguration::class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(
  properties = ["prisonApi.endpoint.url=http://localhost:9097"],
  classes = [TestWebClientConfig::class, PrisonApiClient::class]
)
class PrisonApiIntegrationTest(
  @Autowired private val prisonApiClient: PrisonApiClient
) {

  private val prisonApiMockServer = PrisonApiMockServer("unused", mapper)

  @MockkBean(name = "prisonApiTimeoutCounter")
  private lateinit var prisonApiTimeoutCounter: Counter

  @BeforeAll
  fun startMockServer() {
    prisonApiMockServer.start()
  }

  @AfterAll
  fun stopMockServer() {
    prisonApiMockServer.stop()
  }

  @BeforeEach
  fun resetMocks() {
    prisonApiMockServer.resetAll()
  }

  private val nomsNumber1 = randomNoms()
  private val nomsNumber2 = randomNoms()
  private val nomsNumbers = setOf(nomsNumber1, nomsNumber2)

  @Test
  fun `can retrieve latest inbound movements for set of nomsNumbers`() {
    prisonApiMockServer.latestMovementsRespondsWith(
      nomsNumbers,
      listOf(
        Movement(nomsNumber1.value, LocalDate.now(), LocalTime.now()),
        Movement(nomsNumber1.value, LocalDate.now(), LocalTime.now())
      ),
      false
    )

    val result = prisonApiClient.latestInboundMovements(nomsNumbers)

    assertThat(result, hasSize(equalTo(2)))
  }

  @Test
  fun `handle timeout from client`() {
    every { prisonApiTimeoutCounter.increment() } just Runs

    prisonApiMockServer.delayPost("/api/movements/offenders/?latestOnly=true&movementTypes=ADM", 3000)

    val exception = assertThrows<RuntimeException> {
      prisonApiClient.latestInboundMovements(nomsNumbers)
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientTimeoutException::class.java))
    assertThat(exception.cause!!.message, equalTo("PrisonApiClient: [java.util.concurrent.TimeoutException]"))

    verify(exactly = 1) { prisonApiTimeoutCounter.increment() }
  }

  @Test
  fun `handle exception from client`() {
    prisonApiMockServer.stubPostWithException("/api/movements/offenders/?latestOnly=true&movementTypes=ADM")

    val exception = assertThrows<RuntimeException> {
      prisonApiClient.latestInboundMovements(nomsNumbers)
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientException::class.java))
    assertThat(
      exception.cause!!.message,
      equalTo("PrisonApiClient: [200 OK from POST http://localhost:9097/api/movements/offenders/?latestOnly=true&movementTypes=ADM; nested exception is reactor.netty.http.client.PrematureCloseException: Connection prematurely closed DURING response]")
    )
  }
}
