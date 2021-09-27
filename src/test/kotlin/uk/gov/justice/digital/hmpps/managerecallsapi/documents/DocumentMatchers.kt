package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallClassPathResource

fun byteArrayDocumentDataFor(recallClassPathResource: RecallClassPathResource) =
  byteArrayDocumentDataFor(String(recallClassPathResource.inputStream().readAllBytes()))

fun byteArrayDocumentDataFor(documentContent: ByteArray) =
  byteArrayDocumentDataFor(String(documentContent))

fun byteArrayDocumentDataFor(documentContent: String) =
  isA<ByteArrayDocumentData>(
    has("data", { String(it.byteArray) }, equalTo(documentContent))
  )
