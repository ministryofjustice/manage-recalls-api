package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaPartBRecordRepository")
interface JpaPartBRecordRepository : JpaRepository<PartBRecord, UUID>

@NoRepositoryBean
interface ExtendedPartBRecordRepository : JpaPartBRecordRepository

@Component
class PartBRecordRepository(
  @Qualifier("jpaPartBRecordRepository") @Autowired private val jpaRepository: JpaPartBRecordRepository
) : JpaPartBRecordRepository by jpaRepository, ExtendedPartBRecordRepository
