package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId

@Service
class S3StorageService(
  @Autowired val s3Client: S3Client,
  @Value("\${aws.s3.bucketName}") val bucketName: String
) : S3Service {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun downloadFile(documentId: DocumentId): ByteArray {
    log.debug("downloading file s3://$bucketName/$documentId")
    val request = GetObjectRequest.builder().bucket(bucketName).key(documentId.toString()).build()
    return s3Client.getObject(request).readAllBytes()
  }

  override fun uploadFile(documentId: DocumentId, fileBytes: ByteArray) {
    log.debug("Uploading file to s3://$bucketName/$documentId")
    val request = PutObjectRequest.builder().bucket(bucketName).key(documentId.toString()).build()
    val body = RequestBody.fromBytes(fileBytes)
    s3Client.putObject(request, body)
  }
}
