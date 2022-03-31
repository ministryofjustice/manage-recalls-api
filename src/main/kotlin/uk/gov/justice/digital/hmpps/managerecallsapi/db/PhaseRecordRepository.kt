package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import java.util.UUID

@Repository("jpaPhaseRecordRepository")
interface JpaPhaseRecordRepository : JpaRepository<PhaseRecord, UUID> {
  fun findByRecallIdAndPhase(
    @Param("recallId") recallId: UUID,
    @Param("phase") phase: Phase
  ): PhaseRecord?
}

@NoRepositoryBean
interface ExtendedPhaseRecordRepository : JpaPhaseRecordRepository

@Component
class PhaseRecordRepository(
  @Qualifier("jpaPhaseRecordRepository") @Autowired private val jpaRepository: JpaPhaseRecordRepository
) : JpaPhaseRecordRepository by jpaRepository, ExtendedPhaseRecordRepository
