package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
class S3StorageService(
  @Autowired val s3Client: S3Client,
  @Value("\${aws.s3.bucketName}") val bucketName: String
) : S3Service {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun downloadFile(fileKey: UUID): ByteArray {
    return s3Client.getObject(
      GetObjectRequest.builder().bucket(bucketName).key(fileKey.toString()).build()
    ).readAllBytes()
  }

  override fun uploadFile(fileBytes: ByteArray): S3BulkResponseEntity {
    log.debug("Uploading file to s3://$bucketName")
    val result: S3BulkResponseEntity
    val uuid = UUID.randomUUID()
    s3Client.putObject(
      PutObjectRequest.builder().bucket(bucketName).key(uuid.toString()).build(),
      RequestBody.fromBytes(fileBytes)
    )
      .sdkHttpResponse()
      .also { log.info("uploaded $uuid to $bucketName: ${it.statusCode()}") }
      .let { response ->
        result = S3BulkResponseEntity(
          bucket = bucketName,
          fileKey = uuid,
          successful = response.isSuccessful,
          statusCode = response.statusCode()
        )
      }
    return result
  }
}
