package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import javax.persistence.EntityNotFoundException

@Service
class RecallDocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository
) {

  fun addDocumentToRecall(recallId: RecallId, documentBytes: ByteArray, documentCategory: RecallDocumentCategory) {
    val recall = try {
      recallRepository.getByRecallId(recallId)
    } catch (e: EntityNotFoundException) {
      throw RecallNotFoundError("No recall found with ID '$recallId'", e)
    }

    val fileS3key = s3Service.uploadFile(documentBytes)
    val document = RecallDocument(
      id = fileS3key,
      recallId = recallId.value,
      category = documentCategory
    )
    recallRepository.save(recall.copy(documents = recall.documents.plus(document)))
  }
}
