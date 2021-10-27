package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class VersionedDocumentTest {

  @Test
  fun `versioned document throws exception with unversioned category`() {
    assertThrows<WrongDocumentTypeException> {
      VersionedDocument(
        ::DocumentId.random(),
        ::RecallId.random(),
        RecallDocumentCategory.OTHER,
        "file.txt",
        OffsetDateTime.now()
      )
    }
  }

  @Test
  fun `versioned document accepts versioned category`() {
    VersionedDocument(
      ::DocumentId.random(),
      ::RecallId.random(),
      RecallDocumentCategory.LICENCE,
      "file.txt",
      OffsetDateTime.now()
    )
  }
}
