package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaMissingDocumentRecordRepository")
interface JpaMissingDocumentRecordRepository : JpaRepository<MissingDocumentsRecord, UUID>

@NoRepositoryBean
interface ExtendedMissingDocumentRecordRepository : JpaMissingDocumentRecordRepository

@Component
class MissingDocumentsRecordRepository(
  @Qualifier("jpaMissingDocumentRecordRepository") @Autowired private val jpaRepository: JpaMissingDocumentRecordRepository
) : JpaMissingDocumentRecordRepository by jpaRepository, ExtendedMissingDocumentRecordRepository
