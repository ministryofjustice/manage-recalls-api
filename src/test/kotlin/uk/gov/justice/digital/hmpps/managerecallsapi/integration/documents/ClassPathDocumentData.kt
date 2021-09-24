package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.InputStreamResource
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.MultipartInputStreamFileResource

data class ClassPathDocumentData(private val path: String) :
  Data {
  override fun data(): InputStreamResource = MultipartInputStreamFileResource(ClassPathResource(path).inputStream)
}
