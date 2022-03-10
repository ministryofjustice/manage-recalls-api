package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.MISSING_DOCUMENTS_EMAIL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.UNCATEGORISED
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate

class DocumentComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )
  private val versionedWithDetailsDocumentCategory = PART_A_RECALL_REPORT
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val fileName = FileName("fileName")
  private val details = "Document details"
  private val addVersionedDocumentRequest = UploadDocumentRequest(versionedWithDetailsDocumentCategory, base64EncodedDocumentContents, fileName, null)

  @Test
  fun `add a document uploads the file to S3 and returns the documentId`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest)

    assertThat(response.documentId, present())
  }

  @Test
  fun `add a document with a virus returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val result = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest, BAD_REQUEST)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(result, equalTo(ErrorResponse(BAD_REQUEST, "VirusFoundException")))
  }

  @Test
  fun `add a versioned with details document at version 2 with blank details returns bad request with body`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest)

    assertThat(response.documentId, present())

    val result = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest.copy(details = " "), BAD_REQUEST)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(result.status, equalTo(BAD_REQUEST))
    assertThat(result.message!!, equalTo("MissingDetailsException: PART_A_RECALL_REPORT version: [2]"))
  }

  @Test
  fun `can upload 2 versions of versioned without details document with null details`() {
    expectNoVirusesWillBeFound()

    val addVersionedWithoutDetailsDocumentRequest = UploadDocumentRequest(MISSING_DOCUMENTS_EMAIL, base64EncodedDocumentContents, fileName, null)

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response1 = authenticatedClient.uploadDocument(recall.recallId, addVersionedWithoutDetailsDocumentRequest)

    assertThat(response1.documentId, present())

    val response2 = authenticatedClient.uploadDocument(recall.recallId, addVersionedWithoutDetailsDocumentRequest)
    assertThat(response2.documentId, present())
    assertThat(response1.documentId, !equalTo(response2.documentId))
  }

  @Test
  fun `upload multiple documents with a 'versioned' category that already exists writes a new document and increments version`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId

    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest.copy(details = details)).documentId
    val secondDocument = authenticatedClient.getDocument(recall.recallId, secondDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(
      secondDocument,
      equalTo(
        GetDocumentResponse(
          secondDocumentId,
          versionedWithDetailsDocumentCategory,
          base64EncodedDocumentContents,
          fileName,
          2,
          details,
          FullName("Bertie Badger"),
          secondDocument.createdDateTime
        )
      )
    )

    val thirdDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest.copy(details = details)).documentId
    val recallDocument = authenticatedClient.getDocument(recall.recallId, thirdDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(firstDocumentId, !equalTo(thirdDocumentId))
    assertThat(
      recallDocument,
      equalTo(
        GetDocumentResponse(
          thirdDocumentId,
          versionedWithDetailsDocumentCategory,
          base64EncodedDocumentContents,
          fileName,
          3,
          details,
          FullName("Bertie Badger"),
          recallDocument.createdDateTime
        )
      )
    )

    val recallResponse = authenticatedClient.getRecall(recall.recallId)
    assertThat(recallResponse.documents.size, equalTo(1))
  }

  @Test
  fun `upload two documents with an 'unversioned' category allows both to be persisted and returned on a recall`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val otherDocumentRequest = UploadDocumentRequest(OTHER, base64EncodedDocumentContents, fileName)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, otherDocumentRequest).documentId
    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, otherDocumentRequest).documentId

    val recallResponse = authenticatedClient.getRecall(recall.recallId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(recallResponse.documents.size, equalTo(2))
  }

  @Test
  fun `add a document returns 404 if recall does not exist`() {
    authenticatedClient.uploadDocument(::RecallId.random(), addVersionedDocumentRequest, expectedStatus = NOT_FOUND)
  }

  @Test
  fun `can download an uploaded document`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(versionedWithDetailsDocumentCategory, base64EncodedDocumentContents, fileName, details)
    )

    val response = authenticatedClient.getDocument(recall.recallId, document.documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(
          document.documentId,
          versionedWithDetailsDocumentCategory,
          base64EncodedDocumentContents,
          fileName,
          1,
          details,
          FullName("Bertie Badger"),
          response.createdDateTime
        )
      )
    )
  }

  @Test
  fun `get document returns 404 if the recall exists but the document does not`() {
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.getDocument(recall.recallId, ::DocumentId.random(), expectedStatus = NOT_FOUND)
  }

  @Test
  fun `can update category for an UNCATEGORISED document to versioned`() {
    expectNoVirusesWillBeFound()
    val updatedCategory = PART_A_RECALL_REPORT

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val documentId = authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(UNCATEGORISED, base64EncodedDocumentContents, fileName, details)
    ).documentId

    val document = documentRepository.getByRecallIdAndDocumentId(recall.recallId, documentId)
    document.let {
      assertThat(it.id(), equalTo(documentId))
      assertThat(it.recallId(), equalTo(recall.recallId))
      assertThat(it.category, equalTo(UNCATEGORISED))
      assertThat(it.version, equalTo(null))
      assertThat(it.fileName, equalTo(fileName))
    }

    val result = authenticatedClient.updateDocumentCategory(
      recall.recallId,
      documentId,
      UpdateDocumentRequest(updatedCategory)
    )

    assertThat(result, equalTo(UpdateDocumentResponse(documentId, recall.recallId, updatedCategory, fileName, details)))

    val response = authenticatedClient.getDocument(recall.recallId, documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(
          documentId,
          updatedCategory,
          base64EncodedDocumentContents,
          fileName,
          1,
          details,
          FullName("Bertie Badger"),
          document.createdDateTime
        )
      )
    )
  }

  @Test
  fun `can update category for a UNCATEGORISED document to unversioned`() {
    expectNoVirusesWillBeFound()
    val updatedCategory = OTHER

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val documentId = authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(UNCATEGORISED, base64EncodedDocumentContents, fileName, details)
    ).documentId

    val document = documentRepository.getByRecallIdAndDocumentId(recall.recallId, documentId)
    document.let {
      assertThat(it.id(), equalTo(documentId))
      assertThat(it.recallId(), equalTo(recall.recallId))
      assertThat(it.category, equalTo(UNCATEGORISED))
      assertThat(it.version, equalTo(null))
      assertThat(it.fileName, equalTo(fileName))
    }

    val result = authenticatedClient.updateDocumentCategory(
      recall.recallId,
      documentId,
      UpdateDocumentRequest(updatedCategory)
    )

    assertThat(result, equalTo(UpdateDocumentResponse(documentId, recall.recallId, updatedCategory, fileName, details)))

    val response = authenticatedClient.getDocument(recall.recallId, documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(
          documentId,
          updatedCategory,
          base64EncodedDocumentContents,
          fileName,
          null,
          details,
          FullName("Bertie Badger"),
          document.createdDateTime
        )
      )
    )
  }

  @Test
  fun `can delete uploaded document for Recall being booked`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(PART_A_RECALL_REPORT, base64EncodedDocumentContents, fileName)
    )

    authenticatedClient.deleteDocument(recall.recallId, document.documentId)
  }

  // TODO: there are no documents generated prior to `Assess Recall` - so why this test "for Recall being booked"?
  //  - PS: Answer: for completion since we verify we cant delete an uploaded doc during 'being booked'
  @Test
  fun `can't delete generated document for Recall being booked`() {
    expectNoVirusesWillBeFound()

    gotenbergMockServer.stubGenerateRevocationOrder(documentContents, "Barrie")

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    authenticatedClient.updateRecall(
      recall.recallId,
      UpdateRecallRequest(
        licenceNameCategory = NameFormatCategory.FIRST_LAST,
        currentPrison = PrisonId("MWI"),
        lastReleasePrison = PrisonId("CFI"),
        localPoliceForceId = PoliceForceId("avon-and-somerset"),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(10, 1, 5),
        sentencingCourt = CourtId("CARLCT"),
        indexOffence = "Badgering",
        bookingNumber = "booking number",
        licenceConditionsBreached = "he was a very naughty boy",
        lastReleaseDate = LocalDate.now(),
        inCustodyAtBooking = true,
      )
    )
    val document = authenticatedClient.generateDocument(recall.recallId, REVOCATION_ORDER, FileName("REVOCATION_ORDER.pdf"))

    val response = authenticatedClient.deleteDocument(recall.recallId, document.documentId, BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response, equalTo(ErrorResponse(BAD_REQUEST, "DocumentDeleteException: Unable to delete document [Recall: ${recall.recallId}, Document: ${document.documentId}]: Wrong status [BEING_BOOKED_ON] and/or document category [REVOCATION_ORDER]")))
  }

  @Test
  fun `after deleting a document with a 'versioned' category the previous version is returned`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId
    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest.copy(details = details)).documentId
    val secondDocument = authenticatedClient.getDocument(recall.recallId, secondDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(
      secondDocument,
      equalTo(
        GetDocumentResponse(
          secondDocumentId,
          versionedWithDetailsDocumentCategory,
          base64EncodedDocumentContents,
          fileName,
          2,
          details,
          FullName("Bertie Badger"),
          secondDocument.createdDateTime
        )
      )
    )

    authenticatedClient.deleteDocument(recall.recallId, secondDocumentId)
    val docs = authenticatedClient.getRecallDocuments(recall.recallId, versionedWithDetailsDocumentCategory)
    assertThat(docs.size, equalTo(1))

    val documents = authenticatedClient.getRecall(recall.recallId).documents

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(documents.size, equalTo(1))
    assertThat(
      documents[0],
      equalTo(
        RecallDocument(
          firstDocumentId,
          versionedWithDetailsDocumentCategory,
          fileName,
          1,
          null,
          documents[0].createdDateTime,
          FullName("Bertie Badger")
        )
      )
    )
  }

  @Test
  fun `get all documents of a given category for a recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val category = PART_A_RECALL_REPORT
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(category, base64EncodedDocumentContents, fileName, null)
    )
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(category, base64EncodedDocumentContents, fileName, details)
    )
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(category, base64EncodedDocumentContents, fileName, details)
    )
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(LICENCE, base64EncodedDocumentContents, fileName, null)
    )
    val recallDocuments = authenticatedClient.getRecallDocuments(recall.recallId, category)

    assertThat(recallDocuments.size, equalTo(3))
  }
}
