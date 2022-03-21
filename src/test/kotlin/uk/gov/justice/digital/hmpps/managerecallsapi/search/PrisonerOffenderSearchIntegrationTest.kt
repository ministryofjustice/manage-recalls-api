package uk.gov.justice.digital.hmpps.managerecallsapi.search

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@Import(MetricsAutoConfiguration::class, CompositeMeterRegistryAutoConfiguration::class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(
  properties = ["prisonerSearch.endpoint.url=http://localhost:9092"],
  classes = [TestWebClientConfig::class, PrisonerOffenderSearchClient::class]
)
class PrisonerOffenderSearchIntegrationTest(
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {

  private val prisonerOffenderSearchMockServer = PrisonerOffenderSearchMockServer("unused", mapper)

  @MockkBean(name = "prisonApiTimeoutCounter")
  private lateinit var prisonApiTimeoutCounter: Counter

  @BeforeAll
  fun startMockServer() {
    prisonerOffenderSearchMockServer.start()
  }

  @AfterAll
  fun stopMockServer() {
    prisonerOffenderSearchMockServer.stop()
  }

  @BeforeEach
  fun resetMocks() {
    prisonerOffenderSearchMockServer.resetAll()
  }

  private val nomsNumber = randomNoms()

  @Test
  fun `can retrieve prisoner details for given nomsNumber`() {
    val prisoner = Prisoner(nomsNumber.value, null, "B1234", "Johnny", "Jimbo", "Smith", LocalDate.of(2000, 1, 1))
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(nomsNumber, prisoner, false)

    val result = prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber).block()

    assertThat(result, equalTo(prisoner))
  }

  @Test
  fun `handle timeout from client`() {
    every { prisonApiTimeoutCounter.increment() } just Runs

    prisonerOffenderSearchMockServer.delayGet("/prisoner/$nomsNumber", 3000)

    val exception = assertThrows<RuntimeException> {
      prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber).block()
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientTimeoutException::class.java))
    assertThat(exception.cause!!.message, equalTo("PrisonerOffenderSearchClient: [java.util.concurrent.TimeoutException]"))

    verify(exactly = 1) { prisonApiTimeoutCounter.increment() }
  }

  @Test
  fun `handle exception from client`() {
    prisonerOffenderSearchMockServer.stubGetWithException("/prisoner/$nomsNumber")

    val exception = assertThrows<RuntimeException> {
      prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber).block()
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientException::class.java))
    assertThat(
      exception.cause!!.message,
      equalTo("PrisonerOffenderSearchClient: [200 OK from GET http://localhost:9092/prisoner/$nomsNumber; nested exception is reactor.netty.http.client.PrematureCloseException: Connection prematurely closed DURING response]")
    )
  }
}
