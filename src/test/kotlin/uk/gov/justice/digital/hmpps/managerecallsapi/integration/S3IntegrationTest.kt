package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.config.S3Config
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3StorageService

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [S3Config::class])
@TestPropertySource(
  properties = [
    "aws.credentials.accessKey=ANYTHING_GOES",
    "aws.credentials.secretKey=ANYTHING_GOES",
    "aws.local.endpoint=http://localhost:4566",
    "aws.region=eu-west-2",
    "aws.s3.bucketName=test-manage-recalls-api"
  ]
)
@Import(S3StorageService::class)
class S3IntegrationTest(@Autowired private val s3StorageService: S3StorageService) {

  @Test
  fun `can store and retrieve a file from s3`() {
    val fileBytes = "blah".toByteArray()
    val documentId = ::DocumentId.random()

    s3StorageService.uploadFile(documentId, fileBytes)
    val downloaded = s3StorageService.downloadFile(documentId)

    assertArrayEquals(downloaded, fileBytes)
  }

  @Test
  fun `can store, update and retrieve a file from s3`() {
    val originalFileBytes = randomString().toByteArray()
    val newFileBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()

    s3StorageService.uploadFile(documentId, originalFileBytes)
    s3StorageService.uploadFile(documentId, newFileBytes)
    val downloaded = s3StorageService.downloadFile(documentId)

    assertArrayEquals(downloaded, newFileBytes)
  }
}
