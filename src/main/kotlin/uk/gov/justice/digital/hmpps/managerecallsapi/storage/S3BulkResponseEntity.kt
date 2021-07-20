package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import java.util.UUID

data class S3BulkResponseEntity(
  var bucket: String,
  var fileKey: UUID,
  var successful: Boolean,
  var statusCode: Int
)
