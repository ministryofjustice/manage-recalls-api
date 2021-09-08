package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import java.util.UUID

interface S3Service {

  fun downloadFile(documentId: UUID): ByteArray

  fun uploadFile(documentId: UUID, fileBytes: ByteArray)
}
