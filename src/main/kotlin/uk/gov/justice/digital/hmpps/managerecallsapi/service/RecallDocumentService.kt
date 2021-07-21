package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID
import javax.persistence.EntityNotFoundException

@Service
class RecallDocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun addDocumentToRecall(recallId: UUID, documentBytes: ByteArray, documentCategory: RecallDocumentCategory) {
    val recall = try {
      recallRepository.getById(recallId)
    } catch (e: EntityNotFoundException) {
      throw RecallNotFoundError("No recall found with ID '$recallId'", e)
    }

    val uploadedDocument = s3Service.uploadFile(documentBytes)
    val document = RecallDocument(
      id = uploadedDocument.fileKey,
      recallId = recallId,
      category = documentCategory
    )
    recallRepository.save(recall.copy(documents = recall.documents.plus(document)))
  }
}
