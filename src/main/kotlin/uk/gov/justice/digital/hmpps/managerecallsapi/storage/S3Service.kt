package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId

interface S3Service {

  fun downloadFile(documentId: DocumentId): ByteArray

  fun uploadFile(documentId: DocumentId, fileBytes: ByteArray)
}
