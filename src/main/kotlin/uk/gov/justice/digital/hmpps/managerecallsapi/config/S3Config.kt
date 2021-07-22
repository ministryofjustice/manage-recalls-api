package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config(@Value("\${aws.local.endpoint:#{null}}") val endpointUrl: String?) {

  @Value("\${aws.region}")
  lateinit var awsRegion: String
  @Value("\${aws.credentials.access-key}")
  lateinit var accessKey: String
  @Value("\${aws.credentials.secret-key}")
  lateinit var secretKey: String

  @Bean(destroyMethod = "close")
  fun s3Client(): S3Client {
    val builder = S3Client
      .builder()
      .region(Region.regions().find { region -> region.toString() == awsRegion })
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))

    if (!endpointUrl.isNullOrBlank()) {
      builder.endpointOverride(URI(endpointUrl))
    }
    return builder.build()
  }
}
