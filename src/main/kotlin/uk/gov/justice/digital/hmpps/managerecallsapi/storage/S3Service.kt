package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import java.util.UUID

interface S3Service {

  fun downloadFile(bucketName: String, fileKey: UUID): ByteArray

  fun uploadFile(bucketName: String, fileBytes: ByteArray, fileName: String?): S3BulkResponseEntity
}
