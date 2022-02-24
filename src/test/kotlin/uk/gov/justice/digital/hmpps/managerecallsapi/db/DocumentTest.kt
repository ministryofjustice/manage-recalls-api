package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.config.MissingDetailsException
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
  fun `document throws exception with versioned with details category and null version`() {
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
  fun `document throws exception with versioned without details category and null version`() {
    assertThrows<WrongDocumentTypeException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.NSY_REMOVE_WARRANT_EMAIL,
        FileName("file.txt"),
        null,
        null,
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `document throws exception with versioned without details category, version greater than 2 and null details `() {
    assertThrows<MissingDetailsException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.PART_A_RECALL_REPORT,
        FileName("file.txt"),
        2,
        null,
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `document throws exception with versioned without details category, version greater than 2 and blank details `() {
    assertThrows<MissingDetailsException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.PART_A_RECALL_REPORT,
        FileName("file.txt"),
        2,
        "   ",
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `document accepts versioned without details category null details and version greater than 1`() {
    assertThrows<WrongDocumentTypeException> {
      Document(
        ::DocumentId.random(),
        ::RecallId.random(),
        DocumentCategory.NSY_REMOVE_WARRANT_EMAIL,
        FileName("file.txt"),
        null,
        null,
        OffsetDateTime.now(),
        ::UserId.random()
      )
    }
  }

  @Test
  fun `document accepts versioned with details category`() {
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
