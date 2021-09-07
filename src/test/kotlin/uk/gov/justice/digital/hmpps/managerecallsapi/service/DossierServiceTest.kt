package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service

@Suppress("ReactiveStreamsUnusedPublisher")
internal class DossierServiceTest {

  private val revocationOrderService = mockk<RevocationOrderService>()
  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = DossierService(
    revocationOrderService,
    s3Service,
    recallRepository
  )

  @Test
  fun `get dossier gets part A, license and revocation order for a recall`() {
    val recallId = ::RecallId.random()
    val expectedBytes = randomString().toByteArray()

    // TODO PUD-521 add part A and license
    every { revocationOrderService.getRevocationOrder(recallId) } returns Mono.just(expectedBytes)

    val result = underTest.getDossier(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
      }
      .verifyComplete()
  }
}
