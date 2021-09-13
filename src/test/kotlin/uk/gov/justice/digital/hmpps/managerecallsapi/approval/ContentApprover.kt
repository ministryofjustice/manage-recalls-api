package uk.gov.justice.digital.hmpps.managerecallsapi.approval

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.ApprovalFailed
import org.http4k.testing.ApprovalSource

interface ContentApprover {
  fun assertApproved(content: String)
}

class NamedResourceContentApprover(
  private val name: String,
  private val approvalSource: ApprovalSource
) : ContentApprover {

  override fun assertApproved(content: String) {
    val approved = approvalSource.approvedFor(name)

    with(approved.input()) {
      val actualSource = approvalSource.actualFor(name)
      val actualContent = content.byteInputStream()

      when (this) {
        null -> {
          with(actualContent) {
            if (available() > 0) {
              copyTo(actualSource.output())
              throw ApprovalFailed("No approved content found", actualSource, approved)
            }
          }
        }
        else -> try {
          assertThat(
            this.reader().use { it.readText() },
            equalTo(actualContent.reader().readText())
          )
        } catch (e: AssertionError) {
          actualContent.copyTo(actualSource.output())
          throw AssertionError(ApprovalFailed("Mismatch", actualSource, approved).message + "\n" + e.message)
        }
      }
    }
  }
}
