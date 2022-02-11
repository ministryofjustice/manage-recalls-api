package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaRescindRecordRepository")
interface JpaRescindRecordRepository : JpaRepository<RescindRecord, UUID>

@NoRepositoryBean
interface ExtendedRescindRecordRepository : JpaRescindRecordRepository

@Component
class RescindRecordRepository(
  @Qualifier("jpaRescindRecordRepository") @Autowired private val jpaRepository: JpaRescindRecordRepository
) : JpaRescindRecordRepository by jpaRepository, ExtendedRescindRecordRepository
