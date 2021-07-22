package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import java.util.UUID

interface S3Service {

  fun downloadFile(fileS3Key: UUID): ByteArray

  fun uploadFile(fileBytes: ByteArray): UUID
}
