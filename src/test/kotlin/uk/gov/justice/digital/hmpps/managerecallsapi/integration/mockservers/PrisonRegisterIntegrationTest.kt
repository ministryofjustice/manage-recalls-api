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
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson.mapper
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.Prison
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.PrisonRegisterClient

@ExtendWith(SpringExtension::class)
@TestInstance(PER_CLASS)
@SpringBootTest(
  properties = ["prisonRegister.endpoint.url=http://localhost:9094"],
  classes = [WebClientConfig::class, PrisonRegisterClient::class]
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

    assertThat(result, hasSize(equalTo(6)))
  }

  @Test
  fun `can retrieve stubbed prison by id`() {
    val prisonId = PrisonId("MWI")
    val prison = Prison(prisonId, PrisonName("Medway (STC)"), true)
    prisonRegisterMockServer.stubPrison(prison)

    val result = prisonRegisterClient.findPrisonById(prisonId).block()

    assertThat(result, present(equalTo(prison)))
  }

  private fun prisonIds(): List<PrisonId> {
    return listOf(
      PrisonId("AAA"),
      PrisonId("XXX"),
      PrisonId("YYY"),
    )
  }

  @ParameterizedTest(name = "find prison with id {0}")
  @MethodSource("prisonIds")
  fun `can retrieve find prison by id using response template`(prisonId: PrisonId) {
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
}
