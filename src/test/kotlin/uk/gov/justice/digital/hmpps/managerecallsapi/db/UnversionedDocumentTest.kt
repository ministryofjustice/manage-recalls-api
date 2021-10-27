package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class UnversionedDocumentTest {

  @Test
  fun `unversioned document throws exception with a versioned category`() {
    assertThrows<WrongDocumentTypeException> {
      UnversionedDocument(
        ::DocumentId.random(),
        ::RecallId.random(),
        RecallDocumentCategory.PART_A_RECALL_REPORT,
        "file.txt",
        OffsetDateTime.now()
      )
    }
  }

  @Test
  fun `unversioned document accepts unversioned category`() {
    UnversionedDocument(
      ::DocumentId.random(),
      ::RecallId.random(),
      RecallDocumentCategory.UNCATEGORISED,
      "file.txt",
      OffsetDateTime.now()
    )
  }
}
