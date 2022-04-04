package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.db.JpaPhaseRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhaseRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("db-test")
class PhaseRecordRepositoryIntegrationTest(
  @Qualifier("jpaPhaseRecordRepository") @Autowired private val jpaRepository: JpaPhaseRecordRepository,
) : IntegrationTestBase() {
  private val underTest = PhaseRecordRepository(jpaRepository)

  @MockkBean
  private lateinit var fixedClock: Clock

  @BeforeEach
  fun `set up clock`() {
    every { fixedClock.instant() } returns OffsetDateTime.now().toInstant()
    every { fixedClock.zone } returns ZoneId.of("UTC")
  }

  @Test
  @Transactional
  fun `can get summary stats by phase`() {
    recallRepository.save(recall, currentUserId)
    underTest.save(PhaseRecord(::PhaseRecordId.random(), recallId, Phase.BOOK, currentUserId, OffsetDateTime.now(fixedClock).minusMinutes(5), currentUserId, OffsetDateTime.now(fixedClock).minusMinutes(4)))
    underTest.save(PhaseRecord(::PhaseRecordId.random(), recallId, Phase.ASSESS, currentUserId, OffsetDateTime.now(fixedClock).minusMinutes(3), currentUserId, OffsetDateTime.now(fixedClock).minusMinutes(1)))

    val summary = underTest.summaryByPhaseSince(LocalDate.EPOCH)

    assertThat(summary.size, equalTo(2))
    val bookSummary = summary.first { it.phase == Phase.BOOK }
    assertThat(bookSummary.count, equalTo(1))
    assertThat(bookSummary.averageDuration, equalTo(60L))
    val assessSummary = summary.first { it.phase == Phase.ASSESS }
    assertThat(assessSummary.count, equalTo(1))
    assertThat(assessSummary.averageDuration, equalTo(120L))
  }
}
