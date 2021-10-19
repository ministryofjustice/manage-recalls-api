package uk.gov.justice.digital.hmpps.managerecallsapi.health

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.health.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClamAVConfig
import xyz.capybara.clamav.ClamavClient
import xyz.capybara.clamav.ClamavException
import java.net.ConnectException

class ClamAVHealthTest {
  @Test
  fun `ping is called when clamAvEnabled`() {
    val clamAVConfig = mockk<ClamAVConfig>()
    val clamavClient = mockk<ClamavClient>()
    every { clamAVConfig.clamavClient() } returns clamavClient
    every { clamavClient.ping() } just runs

    val clamAVHealth = ClamAVHealth(true, clamAVConfig)
    val response = clamAVHealth.health()

    assertThat(response.status, equalTo(Status.UP))

    verify { clamavClient.ping() }
  }

  @Test
  fun `ping is not called when clamAvEnabled=false`() {
    val clamAVConfig = mockk<ClamAVConfig>()
    val clamavClient = mockk<ClamavClient>()
    every { clamAVConfig.clamavClient() } returns clamavClient

    val clamAVHealth = ClamAVHealth(false, clamAVConfig)
    val response = clamAVHealth.health()

    assertThat(response.status, equalTo(Status.UP))

    verify(exactly = 0) { clamavClient.ping() }
  }

  @Test
  fun `Status is Down when ping fails`() {
    val clamAVConfig = mockk<ClamAVConfig>()
    val clamavClient = mockk<ClamavClient>()
    every { clamAVConfig.clamavClient() } returns clamavClient
    every { clamavClient.ping() } throws ClamavException(ConnectException("FAIL"))

    val clamAVHealth = ClamAVHealth(true, clamAVConfig)
    val response = clamAVHealth.health()

    assertThat(response.status, equalTo(Status.DOWN))

    verify { clamavClient.ping() }
  }
}
