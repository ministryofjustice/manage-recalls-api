package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA

fun byteArrayDocumentDataFor(documentContent: ByteArray) =
  byteArrayDocumentDataFor(String(documentContent))

fun byteArrayDocumentDataFor(documentContent: String) =
  isA<ByteArrayDocumentData>(
    has("data", { String(it.byteArray) }, equalTo(documentContent))
  )
