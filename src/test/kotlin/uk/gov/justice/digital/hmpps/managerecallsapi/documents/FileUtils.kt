package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource

fun base64EncodedFileContents(filePath: String): String =
  ClassPathResource(filePath).file.readBytes().encodeToBase64String()

fun RecallClassPathResource.readText(): String = this.inputStream().reader().use { it.readText() }
