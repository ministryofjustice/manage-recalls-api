package uk.gov.justice.digital.hmpps.managerecallsapi.approval

import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

abstract class ApprovalTestCase {

  @RegisterExtension
  @JvmField val approvalTest = TestApprover()

  operator fun <T> ContentApprover.invoke(expectedStatus: HttpStatus, action: () -> ResponseEntity<T>) {
    assertApproved(expectedStatus, action)
  }
}
