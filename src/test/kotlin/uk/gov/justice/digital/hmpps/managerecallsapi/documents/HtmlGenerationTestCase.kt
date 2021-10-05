package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ApprovalTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ThymeleafConfig

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ThymeleafConfig::class])
abstract class HtmlGenerationTestCase : ApprovalTestCase()
