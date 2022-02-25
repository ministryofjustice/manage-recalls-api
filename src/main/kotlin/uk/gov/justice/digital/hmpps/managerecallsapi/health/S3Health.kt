package uk.gov.justice.digital.hmpps.managerecallsapi.health

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
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
  @Value("\${aws.s3.bucketName}") val bucketName: String,
  @Autowired private val meterRegistry: MeterRegistry
) : HealthIndicator {

  private val componentName = "s3"

  override fun health(): Health {
    return try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).sdkHttpResponse()
        .let { response ->
          if (response.isSuccessful) {
            meterRegistry.gauge("upstream_healthcheck", Tags.of("service", componentName), 1)
            Health.up().withDetail("status", response.statusCode()).build()
          } else {
            meterRegistry.gauge("upstream_healthcheck", Tags.of("service", componentName), 0)
            Health.down().withDetail("status", response.statusCode()).build()
          }
        }
    } catch (e: NoSuchBucketException) {
      Health.down(e).build()
    }
  }
}
