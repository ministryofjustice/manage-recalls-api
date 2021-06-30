package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RecallRepository : JpaRepository<Recall, UUID>