package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import java.time.LocalDate
import java.util.UUID

@Repository("jpaPhaseRecordRepository")
interface JpaPhaseRecordRepository : JpaRepository<PhaseRecord, UUID> {
  fun findByRecallIdAndPhase(
    @Param("recallId") recallId: UUID,
    @Param("phase") phase: Phase
  ): PhaseRecord?

  @Query(
    value = """
      select phase, round(avg(duration)) as averageDuration, count(*) as count from (
        select phase, extract(epoch FROM (ended_date_time)) - extract(epoch FROM (started_date_time)) as duration 
        from phase_record
        where ended_date_time is not null
         AND ended_date_time > started_date_time) as records
      group by phase""",
    nativeQuery = true
  )
  fun summaryByPhaseSince(fromDate: LocalDate): List<PhaseAverageDuration>
}

@NoRepositoryBean
interface ExtendedPhaseRecordRepository : JpaPhaseRecordRepository

@Component
class PhaseRecordRepository(
  @Qualifier("jpaPhaseRecordRepository") @Autowired private val jpaRepository: JpaPhaseRecordRepository
) : JpaPhaseRecordRepository by jpaRepository, ExtendedPhaseRecordRepository

interface PhaseAverageDuration {
  val phase: Phase
  val averageDuration: Long
  val count: Long
}
