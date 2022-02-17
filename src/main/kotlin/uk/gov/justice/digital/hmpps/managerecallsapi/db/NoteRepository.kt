package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaNoteRepository")
interface JpaNoteRepository : JpaRepository<Note, UUID>

@NoRepositoryBean
interface ExtendedNoteRepository : JpaNoteRepository

@Component
class NoteRepository(
  @Qualifier("jpaNoteRepository") @Autowired private val jpaRepository: JpaNoteRepository
) : JpaNoteRepository by jpaRepository, ExtendedNoteRepository
