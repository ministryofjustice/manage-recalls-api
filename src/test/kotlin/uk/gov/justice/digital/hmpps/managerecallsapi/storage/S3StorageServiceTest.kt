package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.util.UUID

class S3StorageServiceTest {

  private val bucketName = "bucket-name"
  private val s3Client = mockk<S3Client>()

  private val underTest = S3StorageService(s3Client, bucketName)

  @Test
  fun `can store file in s3`() {
    val fileBytes = "blah".toByteArray()
    val documentId = UUID.randomUUID()

    every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns
      PutObjectResponse.builder().build()

    underTest.uploadFile(documentId, fileBytes)

    verify {
      s3Client.putObject(
        withArg<PutObjectRequest> { request ->
          assertThat(request.bucket(), equalTo(bucketName))
          assertThat(request.key(), equalTo(documentId.toString()))
        },
        any<RequestBody>()
      )
    }
  }

  @Test
  fun `can download file from s3`() {
    val responseInputStream = mockk<ResponseInputStream<GetObjectResponse>>()
    val fileBytes = "some pdf content".toByteArray()

    every { responseInputStream.readAllBytes() } returns fileBytes
    every { s3Client.getObject(any<GetObjectRequest>()) } returns responseInputStream

    val response = underTest.downloadFile(UUID.randomUUID())

    assertThat(response, equalTo(fileBytes))
  }
}
