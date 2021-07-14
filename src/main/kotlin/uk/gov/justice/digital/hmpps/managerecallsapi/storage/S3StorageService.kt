package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
class S3StorageService(var s3Client: S3Client) : S3Service {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun downloadFile(bucketName: String, fileKey: UUID): ByteArray {
    return s3Client.getObject(
      GetObjectRequest.builder().bucket(bucketName).key(fileKey.toString()).build()
    ).readAllBytes()
  }

  override fun uploadFile(bucketName: String, fileBytes: ByteArray, fileName: String?): S3BulkResponseEntity {
    logger.info("Uploading $fileName to $bucketName ")
    val result: S3BulkResponseEntity
    run {
      val originFileName: String? = fileName
      val uuid = UUID.randomUUID()
      s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(uuid.toString()).build(),
        RequestBody.fromBytes(fileBytes)
      )
        .sdkHttpResponse()
        .also { x -> logger.info("AWS S3 uploadFile $originFileName as $uuid to $bucketName code ${x.statusCode()}") }
        .let { response ->
          result = S3BulkResponseEntity(
            bucket = bucketName,
            fileKey = uuid,
            originFileName = originFileName ?: "no name",
            successful = response.isSuccessful,
            statusCode = response.statusCode()
          )
        }
    }
    return result
  }
}
