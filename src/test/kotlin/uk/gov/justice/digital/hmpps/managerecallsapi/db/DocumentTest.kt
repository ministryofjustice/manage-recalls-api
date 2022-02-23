package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class DocumentTest {

  @Test
  fun `document throws exception with unversioned category and version`() {
    assertThrows<WrongDocumentTypeException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.OTHER,
        FileName("file.txt"),
        1,
        null,
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `document throws exception with versioned category and null version`() {
    assertThrows<WrongDocumentTypeException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.LICENCE,
        FileName("file.txt"),
        null,
        null,
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `versioned document accepts versioned category`() {
    Document(
      ::DocumentId.random(),
      ::RecallId.random(),
      DocumentCategory.LICENCE,
      FileName("file.txt"),
      1,
      null,
      OffsetDateTime.now(),
      ::UserId.random()
    )
  }
}
