package uk.gov.justice.digital.hmpps.managerecallsapi.approval

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.ApprovalFailed
import org.http4k.testing.ApprovalSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson

interface ContentApprover {
  fun assertApproved(content: String)
  fun <T> assertApproved(expectedStatus: HttpStatus, action: () -> ResponseEntity<T>)
}

class NamedResourceContentApprover(
  private val name: String,
  private val approvalSource: ApprovalSource
) : ContentApprover {

  override fun assertApproved(content: String) {
    val approved = approvalSource.approvedFor(name)

    with(approved.input()) {
      val actualSource = approvalSource.actualFor(name)

      when (this) {
        null -> {
          with(content.byteInputStream()) {
            if (available() > 0) {
              copyTo(actualSource.output())
              throw ApprovalFailed("No approved content found", actualSource, approved)
            }
          }
        }
        else -> try {
          assertThat(
            content.byteInputStream().reader().readText(),
            equalTo(this.reader().use { it.readText() })
          )
        } catch (e: AssertionError) {
          content.byteInputStream().copyTo(actualSource.output())
          throw AssertionError(ApprovalFailed("Mismatch", actualSource, approved).message + "\n" + e.message)
        }
      }
    }
  }

  override fun <T> assertApproved(expectedStatus: HttpStatus, action: () -> ResponseEntity<T>) {
    with(action()) {
      assertThat(this.statusCode, equalTo(expectedStatus))
      assertApproved(ManageRecallsApiJackson.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.body))
    }
  }
}
