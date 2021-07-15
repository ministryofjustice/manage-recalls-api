package uk.gov.justice.digital.hmpps.managerecallsapi.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.util.UUID

class S3StorageServiceTest {

  val s3Client = mockk<S3Client>()

  val underTest: S3StorageService = S3StorageService(s3Client)

  @Test
  fun `can store file in s3`() {
    val sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(200).build()
    val putObjectResponseBuilder = PutObjectResponse.builder()
    putObjectResponseBuilder.sdkHttpResponse(sdkHttpFullResponse).build()

    every {
      s3Client.putObject(
        any<PutObjectRequest>(),
        any<RequestBody>()
      )
    } returns putObjectResponseBuilder.build()

    val result = underTest.uploadFile("bucket-name", "blah".toByteArray(), "file-a.pdf")

    assertThat(result, equalTo(S3BulkResponseEntity("bucket-name", result.fileKey, "file-a.pdf", true, 200)))
  }

  @Test
  fun `can download file from s3`() {
    val responseInputStream = mockk<ResponseInputStream<GetObjectResponse>>()
    val fileContent = "some pdf content".toByteArray()

    every { responseInputStream.readAllBytes() } returns fileContent
    every { s3Client.getObject(any<GetObjectRequest>()) } returns responseInputStream

    val response = underTest.downloadFile("bucket-name", UUID.randomUUID())

    assertThat(response, equalTo(fileContent))
  }
}
