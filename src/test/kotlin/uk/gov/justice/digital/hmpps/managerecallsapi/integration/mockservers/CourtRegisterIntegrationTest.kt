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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.TestWebClientConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court
import uk.gov.justice.digital.hmpps.managerecallsapi.register.TimeoutHandlingWebClient

@ExtendWith(SpringExtension::class)
@Import(MetricsAutoConfiguration::class, CompositeMeterRegistryAutoConfiguration::class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(
  properties = ["courtRegister.endpoint.url=http://localhost:9095"],
  classes = [TestWebClientConfig::class]
)
class CourtRegisterIntegrationTest(
  @Autowired courtRegisterTestWebClient: WebClient
) {

  private val courtRegisterTimeoutCounter: Counter = mockk()

  private val courtRegisterClient: CourtRegisterClient =
    CourtRegisterClient(TimeoutHandlingWebClient(courtRegisterTestWebClient, 1, courtRegisterTimeoutCounter))

  private val courtRegisterMockServer = CourtRegisterMockServer(mapper)

  @BeforeAll
  fun startMockServer() {
    courtRegisterMockServer.start()
  }

  @AfterAll
  fun stopMockServer() {
    courtRegisterMockServer.stop()
  }

  @BeforeEach
  fun resetMocks() {
    courtRegisterMockServer.resetAll()
    courtRegisterClient.clearCache()
  }

  @Test
  fun `can retrieve all courts`() {
    courtRegisterMockServer.stubCourts()

    val result = courtRegisterClient.getAllCourts().block()!!

    assertThat(result, hasSize(equalTo(5)))
  }

  private fun courts(): List<Court> = courtRegisterMockServer.courts

  @ParameterizedTest(name = "find court {0}")
  @MethodSource("courts")
  fun `can find court by id`(court: Court) {
    courtRegisterMockServer.stubCourts()

    val result = courtRegisterClient.findById(court.courtId).block()
    assertThat(result, present(equalTo(court)))
  }

  @Test
  fun `find court returns empty if court does not exist`() {
    courtRegisterMockServer.stubCourts()

    val result = courtRegisterClient.findById(CourtId("XXX")).block()

    assertThat(result, absent())
  }

  @Test
  fun `handle timeout from client`() {
    every { courtRegisterTimeoutCounter.increment() } just Runs

    courtRegisterMockServer.delaySearch("/courts/all", 3000)

    val exception = assertThrows<RuntimeException> {
      courtRegisterClient.getAllCourts().block()!!
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientTimeoutException::class.java))
    assertThat(exception.cause!!.message, equalTo("CourtRegisterClient: [java.util.concurrent.TimeoutException]"))

    verify(exactly = 1) { courtRegisterTimeoutCounter.increment() }
  }

  @Test
  fun `handle exception from client`() {
    courtRegisterMockServer.stubCallWithException("/courts/all")

    val exception = assertThrows<RuntimeException> {
      courtRegisterClient.getAllCourts().block()!!
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientException::class.java))
    assertThat(exception.cause!!.message, equalTo("CourtRegisterClient: [200 OK from GET http://localhost:9095/courts/all; nested exception is reactor.netty.http.client.PrematureCloseException: Connection prematurely closed DURING response]"))
  }
}
