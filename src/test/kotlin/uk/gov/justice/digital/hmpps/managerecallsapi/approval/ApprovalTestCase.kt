package uk.gov.justice.digital.hmpps.managerecallsapi.approval

import org.junit.jupiter.api.extension.RegisterExtension

abstract class ApprovalTestCase {

  @RegisterExtension
  @JvmField val approvalTest = TestApprover()
}
