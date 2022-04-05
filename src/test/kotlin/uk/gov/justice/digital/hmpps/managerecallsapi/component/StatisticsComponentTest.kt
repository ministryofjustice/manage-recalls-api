package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.LocalDate

class StatisticsComponentTest : ComponentTestBase() {
  private val nomsNumber = randomNoms()
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )

  @BeforeEach
  fun `clear statistics`() {
    phaseRecordRepository.deleteAll()
  }

  @Test
  fun `Getting summary statistics returns average duration for ended phases`() {
    val phase = Phase.ASSESS
    val recall1 = authenticatedClient.bookRecall(bookRecallRequest)
    authenticatedClient.endPhase(recall1.recallId, Phase.BOOK, false)
    authenticatedClient.startPhase(recall1.recallId, phase)
    authenticatedClient.endPhase(recall1.recallId, phase, false)
    val recall2 = authenticatedClient.bookRecall(bookRecallRequest)
    authenticatedClient.endPhase(recall2.recallId, Phase.BOOK, false)
    authenticatedClient.startPhase(recall2.recallId, phase)
    authenticatedClient.endPhase(recall2.recallId, phase, false)

    val stats = authenticatedClient.summaryStatistics()
    assertThat(stats.lastSevenDays.size, equalTo(2))
    assertThat(stats.lastSevenDays.first { it.phase == Phase.BOOK }.count, equalTo(2))
    assertThat(stats.lastSevenDays.first { it.phase == phase }.count, equalTo(2))
    assertThat(stats.overall.size, equalTo(2))
    assertThat(stats.overall.first { it.phase == Phase.BOOK }.count, equalTo(2))
    assertThat(stats.overall.first { it.phase == phase }.count, equalTo(2))
  }

  @Test
  fun `Getting summary statistics returns empty lists when no records`() {
    val stats = authenticatedClient.summaryStatistics()
    assertThat(stats.lastSevenDays.size, equalTo(0))
    assertThat(stats.overall.size, equalTo(0))
  }
}
