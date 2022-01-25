package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaLastKnownAddressRepository")
interface JpaLastKnownAddressRepository : JpaRepository<LastKnownAddress, UUID>

@NoRepositoryBean
interface ExtendedLastKnownAddressRepository : JpaLastKnownAddressRepository

@Component
class LastKnownAddressRepository(
  @Qualifier("jpaLastKnownAddressRepository") @Autowired private val jpaRepository: JpaLastKnownAddressRepository
) : JpaLastKnownAddressRepository by jpaRepository, ExtendedLastKnownAddressRepository
