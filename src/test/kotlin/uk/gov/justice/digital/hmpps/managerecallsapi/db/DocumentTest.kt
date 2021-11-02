package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class DocumentTest {

  @Test
  fun `versioned document throws exception with unversioned category`() {
    assertThrows<WrongDocumentTypeException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        RecallDocumentCategory.OTHER,
        "file.txt",
        1,
        OffsetDateTime.now()
      )
    }
  }

  @Test
  fun `versioned document accepts versioned category`() {
    Document(
      ::DocumentId.random(),
      ::RecallId.random(),
      RecallDocumentCategory.LICENCE,
      "file.txt",
      1,
      OffsetDateTime.now()
    )
  }
}
