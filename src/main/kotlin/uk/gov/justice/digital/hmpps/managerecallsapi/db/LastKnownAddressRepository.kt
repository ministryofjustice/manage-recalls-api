package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.config.LastKnownAddressNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.util.UUID

@Repository("jpaLastKnownAddressRepository")
interface JpaLastKnownAddressRepository : JpaRepository<LastKnownAddress, UUID> {
  fun findByRecallIdAndId(
    @Param("recallId") recallId: UUID,
    @Param("id") lastKnownAddressId: UUID
  ): LastKnownAddress?
}

@NoRepositoryBean
interface ExtendedLastKnownAddressRepository : JpaLastKnownAddressRepository

@Component
class LastKnownAddressRepository(
  @Qualifier("jpaLastKnownAddressRepository") @Autowired private val jpaRepository: JpaLastKnownAddressRepository
) : JpaLastKnownAddressRepository by jpaRepository, ExtendedLastKnownAddressRepository {

  fun deleteByRecallIdAndLastKnownAddressId(recallId: RecallId, lastKnownAddressId: LastKnownAddressId) {
    findByRecallIdAndId(recallId.value, lastKnownAddressId.value)?.let {
      deleteById(lastKnownAddressId.value)
    }
      ?: throw LastKnownAddressNotFoundException(recallId, lastKnownAddressId)
  }
}
