package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import java.util.UUID

data class S3BulkResponseEntity constructor(
  var bucket: String,
  var fileKey: UUID,
  var originFileName: String,
  var successful: Boolean,
  var statusCode: Int
)
