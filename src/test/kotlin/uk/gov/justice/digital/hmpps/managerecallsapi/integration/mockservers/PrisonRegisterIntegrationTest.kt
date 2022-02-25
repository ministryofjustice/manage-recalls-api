package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson.mapper
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.TestWebClientConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.register.PrisonRegisterClient

@ExtendWith(SpringExtension::class)
@TestInstance(PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(
  properties = ["prisonRegister.endpoint.url=http://localhost:9094"],
  classes = [TestWebClientConfig::class, PrisonRegisterClient::class]
)
class PrisonRegisterIntegrationTest(
  @Autowired private val prisonRegisterClient: PrisonRegisterClient
) {

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
  }

  @Test
  fun `can retrieve all prisons`() {
    prisonRegisterMockServer.stubPrisons()

    val result = prisonRegisterClient.getAllPrisons().block()!!

    assertThat(result, hasSize(equalTo(7)))
  }

  @Test
  fun `can retrieve stubbed prison by id`() {
    val prisonId = PrisonId("MWI")
    val prison = Prison(prisonId, PrisonName("Medway (STC)"), true)
    prisonRegisterMockServer.stubPrison(prison)

    val result = prisonRegisterClient.findPrisonById(prisonId).block()!!

    assertThat(result, present(equalTo(prison)))
  }

  @Test
  fun `can retrieve find prison by id using response template`() {
    val prisonId = PrisonId("AAA")
    prisonRegisterMockServer.stubFindAnyPrisonById()

    val result = prisonRegisterClient.findPrisonById(prisonId).block()
    assertThat(
      result,
      present(equalTo(Prison(prisonId, PrisonName("Test prison $prisonId"), true)))
    )
  }

  @Test
  fun `get prison returns empty if prison does not exist`() {
    val result = prisonRegisterClient.findPrisonById(PrisonId("XXX")).block()

    assertThat(result, absent())
  }

  @Test
  fun `handle timeout from client`() {
    prisonRegisterMockServer.delaySearch("/prisons", 2500)
    val exception = assertThrows<RuntimeException> {
      prisonRegisterClient.getAllPrisons().block()
    }
    assertThat(exception.cause!!.javaClass, equalTo(ClientTimeoutException::class.java))
    assertThat(exception.cause!!.message, equalTo("PrisonRegisterClient: [java.util.concurrent.TimeoutException]"))
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
