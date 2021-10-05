package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

@Component("s3")
class S3Health(
  @Autowired val s3Client: S3Client,
  @Value("\${aws.s3.bucketName}") val bucketName: String
) : HealthIndicator {

  override fun health(): Health {
    return try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).sdkHttpResponse()
        .let { response ->
          if (response.isSuccessful) {
            Health.up().withDetail("status", response.statusCode()).build()
          } else {
            Health.down().withDetail("status", response.statusCode()).build()
          }
        }
    } catch (e: NoSuchBucketException) {
      Health.down(e).build()
    }
  }
}
