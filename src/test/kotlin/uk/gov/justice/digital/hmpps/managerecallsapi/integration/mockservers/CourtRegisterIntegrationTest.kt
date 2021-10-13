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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient.Court

@ExtendWith(SpringExtension::class)
@TestInstance(PER_CLASS)
@SpringBootTest(
  properties = ["courtRegister.endpoint.url=http://localhost:9095"],
  classes = [WebClientConfig::class, CourtRegisterClient::class]
)
class CourtRegisterIntegrationTest(
  @Autowired private val courtRegisterClient: CourtRegisterClient
) {

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
  }

  @Test
  fun `can retrieve all courts`() {
    courtRegisterMockServer.stubCourts()

    val result = courtRegisterClient.getAllCourts().block()!!

    assertThat(result, hasSize(equalTo(4)))
  }

  @Test
  fun `can find court by id`() {
    val courtId = CourtId("COURT")
    val court = Court(courtId, CourtName("Test Court"))
    courtRegisterMockServer.stub(court)

    val result = courtRegisterClient.findById(courtId).block()

    assertThat(result, present(equalTo(court)))
  }

  private fun courtIds(): List<CourtId> =
    listOf(
      CourtId("AAA"),
      CourtId("XXX"),
      CourtId("YYY"),
    )

  @ParameterizedTest(name = "find court with id {0}")
  @MethodSource("courtIds")
  fun `can find court by any id using response template`(courtId: CourtId) {
    courtRegisterMockServer.stubFindAnyCourtById()

    val result = courtRegisterClient.findById(courtId).block()
    assertThat(
      result,
      present(equalTo(Court(courtId, CourtName("Test court $courtId"))))
    )
  }

  @Test
  fun `find court returns empty if court does not exist`() {
    val result = courtRegisterClient.findById(CourtId("XXX")).block()

    assertThat(result, absent())
  }
}
