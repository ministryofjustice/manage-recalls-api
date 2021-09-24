package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallClassPathResource
import java.util.Base64

fun base64EncodedFileContents(fileName: String): String =
  Base64.getEncoder().encodeToString(ClassPathResource(fileName).file.readBytes())

fun RecallClassPathResource.readText(): String = this.inputStream().reader().use { it.readText() }
