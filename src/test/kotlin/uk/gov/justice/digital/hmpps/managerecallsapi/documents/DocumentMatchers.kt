package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentData

fun inputStreamDocumentDataFor(recallClassPathResource: RecallClassPathResource) =
  inputStreamDocumentDataFor(String(recallClassPathResource.inputStream().readAllBytes()))

fun inputStreamDocumentDataFor(documentContent: String) =
  isA<InputStreamDocumentData>(
    has("data", { String(it.inputStream.readAllBytes()) }, equalTo(documentContent))
  )
