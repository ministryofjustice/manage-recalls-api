package uk.gov.justice.digital.hmpps.managerecallsapi.component.pact.provider

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.MISSING_DOCUMENTS_EMAIL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.NOTE_DOCUMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_EMAIL_FROM_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RESCIND_REQUEST_EMAIL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.UNCATEGORISED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Note
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhaseRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.nomis.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecallWithoutDocuments
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanner
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("unused")
@PactFilter(value = ["^((?!unauthorized).)*\$"])
class ManagerRecallsUiAuthorizedPactTest : ManagerRecallsUiPactTestBase() {
  private val matchedNomsNumber = NomsNumber("A1234AA")
  private val matchedRecallId = ::RecallId.zeroes()
  private val matchedDocumentId = DocumentId(UUID.fromString("11111111-0000-0000-0000-000000000000"))
  private val matchedLastKnownAddressId = LastKnownAddressId(UUID.fromString("22222222-0000-0000-0000-000000000000"))
  private val matchedRescindRecordId = RescindRecordId(UUID.fromString("33333333-0000-0000-0000-000000000000"))
  private val matchedNoteId = NoteId(UUID.fromString("44444444-0000-0000-0000-000000000000"))
  private val matchedPartBRecallId = NoteId(UUID.fromString("55555555-0000-0000-0000-000000000000"))
  private val userIdOnes = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
  private val revocationOrderBytes = ClassPathResource("/document/revocation-order.pdf").file.readBytes()
  private val details = "Random details"

  @MockkBean
  private lateinit var virusScanner: VirusScanner

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    request.removeHeaders(AUTHORIZATION)
    request.addHeader(AUTHORIZATION, "Bearer ${testJwt(userIdOnes, "ROLE_MANAGE_RECALLS")}")
    pactContext.verifyInteraction()
  }

  @BeforeEach
  fun `delete all recall data and set no virus found`() {
    `delete all recall data`()
    every { virusScanner.scan(any()) } returns VirusScanResult.NoVirusFound
  }

  @State("a prisoner exists for NOMS number")
  fun `a prisoner exists for NOMS number`() {
    mockGetPrisoner(matchedNomsNumber)
  }

  private fun mockGetPrisoner(nomsNum: NomsNumber) {
    prisonerOffenderSearchMockServer.getPrisonerByNomsNumberRespondsWith(
      nomsNum,
      Prisoner(
        prisonerNumber = nomsNum.value,
        pncNumber = "98/7654Z",
        croNumber = "1234/56A",
        firstName = "Bobby",
        middleNames = "John",
        lastName = "Badger",
        dateOfBirth = LocalDate.of(1999, 5, 28),
        gender = "Male"
      )
    )
  }

  @State(
    "no state required"
  )
  fun `no state required`() {
  }

  @State(
    "a user exists"
  )
  fun `a user exists`() {
    setupUserDetailsFor(userIdOnes)
  }

  @State(
    "a user and a fully populated recall without documents exists",
    "a user and a fully populated FIXED recall without documents exists",
    "a user and a fully populated Fixed Term recall without documents exists"
  )
  fun `a user and a fully populated Fixed Term recall without documents exists`() {
    val recall = fullyPopulatedRecallWithoutDocuments(matchedRecallId, userIdOnes, FIXED).let {
      it.copy(
        currentPrison = PrisonId("BMI"),
        lastReleasePrison = PrisonId("CFI"),
        sentencingInfo = it.sentencingInfo?.copy(sentencingCourt = CourtId("BANBCT")),
        localPoliceForceId = PoliceForceId("avon-and-somerset"),
        licenceNameCategory = NameFormatCategory.FIRST_LAST,
      )
    }
    mockGetPrisoner(recall.nomsNumber)
    gotenbergMockServer.stubGenerateRevocationOrder(revocationOrderBytes, recall.firstName.value)
    setupUserDetailsFor(userIdOnes)
    setupUserDetailsFor(::UserId.zeroes())
    setupUserDetailsFor(UserId(UUID.fromString("00000000-1111-0000-0000-000000000000")))
    setupUserDetailsFor(UserId(UUID.fromString("00000000-2222-0000-0000-000000000000")))
    recallRepository.save(recall, userIdOnes)
    phaseRecordRepository.save(
      PhaseRecord(
        ::PhaseRecordId.random(),
        matchedRecallId,
        Phase.BOOK,
        userIdOnes,
        OffsetDateTime.now()
      )
    )
  }

  @State(
    "a user and an unassigned fully populated recall exists without documents",
    "a user and an unassigned fully populated Fixed Term recall exists without documents"
  )
  fun `a user and an unassigned fully populated Fixed Term recall exists without documents`() {
    val recall = fullyPopulatedRecallWithoutDocuments(matchedRecallId, userIdOnes, FIXED).copy(
      assignee = null
    )
    setupUserDetailsFor(userIdOnes)
    recallRepository.save(recall, userIdOnes)
  }

  @State(
    "a list of recalls exists"
  )
  fun `a list of recalls exists`() {

    recallRepository.saveAll(
      listOf(
        fullyPopulatedRecallWithoutDocuments(::RecallId.random(), userIdOnes).copy(nomsNumber = matchedNomsNumber),
        fullyPopulatedRecallWithoutDocuments(::RecallId.random(), userIdOnes),
        fullyPopulatedRecallWithoutDocuments(::RecallId.random(), userIdOnes),
      )
    )
  }

  @State(
    "a recall and uncategorised document exist",
    "a Fixed Term recall and uncategorised document exist"
  )
  fun `a Fixed Term recall and uncategorised document exist`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `a document exists`(matchedDocumentId, UNCATEGORISED, FileName("filename.pdf"), null, null)
  }

  @State(
    "a recall and document history exist",
  )
  fun `a Fixed Term recall and document history exist`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    val randomPartAdocumentId = ::DocumentId.random()
    `a document exists`(randomPartAdocumentId, PART_A_RECALL_REPORT, FileName("filename.pdf"), 1, null)
    `a document exists`(matchedDocumentId, LICENCE, FileName("license.pdf"), 1, null)
    `a document exists`(randomPartAdocumentId, PART_A_RECALL_REPORT, FileName("filename.pdf"), 2, "New updated part A")
  }

  @State(
    "a recall with missing document records exists",
    "a Fixed Term recall with missing document records exists",
  )
  fun `a Fixed Term recall with missing document records exist`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `a missing document record and email document exist`(setOf(PART_A_RECALL_REPORT, LICENCE), 1, "missing")
    `a missing document record and email document exist`(setOf(LICENCE), 2, "still missing")
  }

  @State(
    "a recall with last known addresses exists",
    "a Fixed Term recall with last known addresses exists",
  )
  fun `a Fixed Term recall with last known addresses exists`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `a last known address exists`(1, AddressSource.LOOKUP, matchedLastKnownAddressId)
    `a last known address exists`(2, AddressSource.MANUAL)
  }

  @State(
    "a recall with notes exists",
  )
  fun `a recall with notes exists`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `a note exists`(1, matchedNoteId, matchedDocumentId)
    `a note exists`(2)
  }

  @State(
    "a recall with part B records exists",
  )
  fun `a recall with part B records exists`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `a note exists`(1, matchedNoteId, matchedDocumentId)
    `a note exists`(2)
  }

  @State(
    "a recall with open rescind record exists",
    "a Fixed Term recall with open rescind record exists",
  )
  fun `a Fixed Term recall with open rescind record exists`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    `an open rescind record exists`()
  }

  @State(
    "a recall in being booked on state with a document exists"
  )
  fun `a recall in being booked on state with a document exists`() {
    val createdByUserId = ::UserId.random()
    setupUserDetailsFor(createdByUserId)
    recallRepository.save(
      Recall(
        matchedRecallId,
        matchedNomsNumber,
        createdByUserId,
        OffsetDateTime.now(),
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("ABC/1234A"),
        LocalDate.of(1999, 12, 1)
      ),
      createdByUserId
    )
    `a document exists`(matchedDocumentId, UNCATEGORISED, FileName("uncategorised.pdf"), null, null)
  }

  @State("summary statistics exist")
  fun `summary statistics exist`() {
    `a user and a fully populated Fixed Term recall without documents exists`()
    val now = OffsetDateTime.now()
    phaseRecordRepository.findByRecallIdAndPhase(matchedRecallId.value, Phase.BOOK)!!.copy(
      startedDateTime = now.minusMinutes(20),
      endedByUserId = userIdOnes.value,
      endedDateTime = now.minusMinutes(11)
    ).let { phaseRecordRepository.save(it) }
    phaseRecordRepository.save(
      PhaseRecord(
        ::PhaseRecordId.random(),
        matchedRecallId,
        Phase.ASSESS,
        userIdOnes,
        now.minusMinutes(10),
        userIdOnes,
        now.minusMinutes(8),
      )
    )
    phaseRecordRepository.save(
      PhaseRecord(
        ::PhaseRecordId.random(),
        matchedRecallId,
        Phase.DOSSIER,
        userIdOnes,
        now.minusMinutes(7),
        userIdOnes,
        now.minusMinutes(6)
      )
    )
  }

  fun `a document exists`(
    documentId: DocumentId,
    documentCategory: DocumentCategory,
    fileName: FileName,
    version: Int?,
    details: String?
  ) {
    val documentBytes =
      "JVBERi0xLjINJeLjz9MNCjMgMCBvYmoNPDwgDS9MaW5lYXJpemVkIDEgDS9PIDUgDS9IIFsgNzYwIDE1NyBdIA0vTCAzOTA4IA0vRSAzNjU4IA0vTiAxIA0vVCAzNzMxIA0+PiANZW5kb2JqDSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB4cmVmDTMgMTUgDTAwMDAwMDAwMTYgMDAwMDAgbg0KMDAwMDAwMDY0NCAwMDAwMCBuDQowMDAwMDAwOTE3IDAwMDAwIG4NCjAwMDAwMDEwNjggMDAwMDAgbg0KMDAwMDAwMTIyNCAwMDAwMCBuDQowMDAwMDAxNDEwIDAwMDAwIG4NCjAwMDAwMDE1ODkgMDAwMDAgbg0KMDAwMDAwMTc2OCAwMDAwMCBuDQowMDAwMDAyMTk3IDAwMDAwIG4NCjAwMDAwMDIzODMgMDAwMDAgbg0KMDAwMDAwMjc2OSAwMDAwMCBuDQowMDAwMDAzMTcyIDAwMDAwIG4NCjAwMDAwMDMzNTEgMDAwMDAgbg0KMDAwMDAwMDc2MCAwMDAwMCBuDQowMDAwMDAwODk3IDAwMDAwIG4NCnRyYWlsZXINPDwNL1NpemUgMTgNL0luZm8gMSAwIFIgDS9Sb290IDQgMCBSIA0vUHJldiAzNzIyIA0vSURbPGQ3MGY0NmM1YmE0ZmU4YmQ0OWE5ZGQwNTk5YjBiMTUxPjxkNzBmNDZjNWJhNGZlOGJkNDlhOWRkMDU5OWIwYjE1MT5dDT4+DXN0YXJ0eHJlZg0wDSUlRU9GDSAgICAgIA00IDAgb2JqDTw8IA0vVHlwZSAvQ2F0YWxvZyANL1BhZ2VzIDIgMCBSIA0vT3BlbkFjdGlvbiBbIDUgMCBSIC9YWVogbnVsbCBudWxsIG51bGwgXSANL1BhZ2VNb2RlIC9Vc2VOb25lIA0+PiANZW5kb2JqDTE2IDAgb2JqDTw8IC9TIDM2IC9GaWx0ZXIgL0ZsYXRlRGVjb2RlIC9MZW5ndGggMTcgMCBSID4+IA1zdHJlYW0NCkiJYmBg4GVgYPrBAAScFxiwAQ4oLQDE3FDMwODHwKkyubctWLfmpsmimQ5AEYAAAwC3vwe0DWVuZHN0cmVhbQ1lbmRvYmoNMTcgMCBvYmoNNTMgDWVuZG9iag01IDAgb2JqDTw8IA0vVHlwZSAvUGFnZSANL1BhcmVudCAyIDAgUiANL1Jlc291cmNlcyA2IDAgUiANL0NvbnRlbnRzIDEwIDAgUiANL01lZGlhQm94IFsgMCAwIDYxMiA3OTIgXSANL0Nyb3BCb3ggWyAwIDAgNjEyIDc5MiBdIA0vUm90YXRlIDAgDT4+IA1lbmRvYmoNNiAwIG9iag08PCANL1Byb2NTZXQgWyAvUERGIC9UZXh0IF0gDS9Gb250IDw8IC9UVDIgOCAwIFIgL1RUNCAxMiAwIFIgL1RUNiAxMyAwIFIgPj4gDS9FeHRHU3RhdGUgPDwgL0dTMSAxNSAwIFIgPj4gDS9Db2xvclNwYWNlIDw8IC9DczUgOSAwIFIgPj4gDT4+IA1lbmRvYmoNNyAwIG9iag08PCANL1R5cGUgL0ZvbnREZXNjcmlwdG9yIA0vQXNjZW50IDg5MSANL0NhcEhlaWdodCAwIA0vRGVzY2VudCAtMjE2IA0vRmxhZ3MgMzQgDS9Gb250QkJveCBbIC01NjggLTMwNyAyMDI4IDEwMDcgXSANL0ZvbnROYW1lIC9UaW1lc05ld1JvbWFuIA0vSXRhbGljQW5nbGUgMCANL1N0ZW1WIDAgDT4+IA1lbmRvYmoNOCAwIG9iag08PCANL1R5cGUgL0ZvbnQgDS9TdWJ0eXBlIC9UcnVlVHlwZSANL0ZpcnN0Q2hhciAzMiANL0xhc3RDaGFyIDMyIA0vV2lkdGhzIFsgMjUwIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL1RpbWVzTmV3Um9tYW4gDS9Gb250RGVzY3JpcHRvciA3IDAgUiANPj4gDWVuZG9iag05IDAgb2JqDVsgDS9DYWxSR0IgPDwgL1doaXRlUG9pbnQgWyAwLjk1MDUgMSAxLjA4OSBdIC9HYW1tYSBbIDIuMjIyMjEgMi4yMjIyMSAyLjIyMjIxIF0gDS9NYXRyaXggWyAwLjQxMjQgMC4yMTI2IDAuMDE5MyAwLjM1NzYgMC43MTUxOSAwLjExOTIgMC4xODA1IDAuMDcyMiAwLjk1MDUgXSA+PiANDV0NZW5kb2JqDTEwIDAgb2JqDTw8IC9MZW5ndGggMzU1IC9GaWx0ZXIgL0ZsYXRlRGVjb2RlID4+IA1zdHJlYW0NCkiJdJDBTsMwEETv/oo92ohuvXHsJEeggOCEwDfEIU1SCqIJIimIv2dthyJVQpGc0Xo88+xzL5beZ0DgN4IIq6oCzd8sK43amAyK3GKmTQV+J5YXo4VmjDYNYyOW1w8Ez6PQ4JuwfAkJyr+yXNgSSwt+NU+4Kp+rcg4uy9Q1a6MdarLcpgvUeUGh7RBFSLk1f1n+5FgsHJaZttFqA+tKLJhfZ3kEY+VcoHuUfvui2O3kCL9COSwk1Ok3deMEd6srUCVa2Q7Nftf1Ewar5a4nfxuu4v59NcLMGAKXlcjMLtwj1BsTQCITUSK52cC3IoNGDnto6l5VmEv4YAwjO8VWJ+s2DSeGttw/qmA/PZyLu3vY1p9p0MGZIs2iHdZxjwdNSkzedT0pJiW+CWl5H0O7uu2SB1JLn8rHlMkH2F+/xa20Rjp+nAQ39Ec8c1gz7KJ4T3H7uXnuwvSWl178CDAA/bGPlAplbmRzdHJlYW0NZW5kb2JqDTExIDAgb2JqDTw8IA0vVHlwZSAvRm9udERlc2NyaXB0b3IgDS9Bc2NlbnQgOTA1IA0vQ2FwSGVpZ2h0IDAgDS9EZXNjZW50IC0yMTEgDS9GbGFncyAzMiANL0ZvbnRCQm94IFsgLTYyOCAtMzc2IDIwMzQgMTA0OCBdIA0vRm9udE5hbWUgL0FyaWFsLEJvbGQgDS9JdGFsaWNBbmdsZSAwIA0vU3RlbVYgMTMzIA0+PiANZW5kb2JqDTEyIDAgb2JqDTw8IA0vVHlwZSAvRm9udCANL1N1YnR5cGUgL1RydWVUeXBlIA0vRmlyc3RDaGFyIDMyIA0vTGFzdENoYXIgMTE3IA0vV2lkdGhzIFsgMjc4IDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMjc4IDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgDTAgMCAwIDAgMCA3MjIgMCA2MTEgMCAwIDAgMCAwIDAgMCAwIDAgNjY3IDAgMCAwIDYxMSAwIDAgMCAwIDAgMCANMCAwIDAgMCAwIDAgNTU2IDAgNTU2IDYxMSA1NTYgMCAwIDYxMSAyNzggMCAwIDAgODg5IDYxMSA2MTEgMCAwIA0wIDU1NiAzMzMgNjExIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL0FyaWFsLEJvbGQgDS9Gb250RGVzY3JpcHRvciAxMSAwIFIgDT4+IA1lbmRvYmoNMTMgMCBvYmoNPDwgDS9UeXBlIC9Gb250IA0vU3VidHlwZSAvVHJ1ZVR5cGUgDS9GaXJzdENoYXIgMzIgDS9MYXN0Q2hhciAxMjEgDS9XaWR0aHMgWyAyNzggMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDI3OCAwIDI3OCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCANMCAwIDAgNjY3IDAgMCAwIDAgMCAwIDAgMjc4IDAgMCAwIDAgMCAwIDAgMCA3MjIgMCAwIDAgMCAwIDAgMCAwIA0wIDAgMCAwIDAgMCA1NTYgNTU2IDUwMCA1NTYgNTU2IDI3OCAwIDU1NiAyMjIgMCAwIDIyMiA4MzMgNTU2IDU1NiANNTU2IDAgMzMzIDUwMCAyNzggNTU2IDUwMCAwIDAgNTAwIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL0FyaWFsIA0vRm9udERlc2NyaXB0b3IgMTQgMCBSIA0+PiANZW5kb2JqDTE0IDAgb2JqDTw8IA0vVHlwZSAvRm9udERlc2NyaXB0b3IgDS9Bc2NlbnQgOTA1IA0vQ2FwSGVpZ2h0IDAgDS9EZXNjZW50IC0yMTEgDS9GbGFncyAzMiANL0ZvbnRCQm94IFsgLTY2NSAtMzI1IDIwMjggMTAzNyBdIA0vRm9udE5hbWUgL0FyaWFsIA0vSXRhbGljQW5nbGUgMCANL1N0ZW1WIDAgDT4+IA1lbmRvYmoNMTUgMCBvYmoNPDwgDS9UeXBlIC9FeHRHU3RhdGUgDS9TQSBmYWxzZSANL1NNIDAuMDIgDS9UUiAvSWRlbnRpdHkgDT4+IA1lbmRvYmoNMSAwIG9iag08PCANL1Byb2R1Y2VyIChBY3JvYmF0IERpc3RpbGxlciA0LjA1IGZvciBXaW5kb3dzKQ0vQ3JlYXRvciAoTWljcm9zb2Z0IFdvcmQgOS4wKQ0vTW9kRGF0ZSAoRDoyMDAxMDgyOTA5NTUwMS0wNycwMCcpDS9BdXRob3IgKEdlbmUgQnJ1bWJsYXkpDS9UaXRsZSAoVGhpcyBpcyBhIHRlc3QgUERGIGRvY3VtZW50KQ0vQ3JlYXRpb25EYXRlIChEOjIwMDEwODI5MDk1NDU3KQ0+PiANZW5kb2JqDTIgMCBvYmoNPDwgDS9UeXBlIC9QYWdlcyANL0tpZHMgWyA1IDAgUiBdIA0vQ291bnQgMSANPj4gDWVuZG9iag14cmVmDTAgMyANMDAwMDAwMDAwMCA2NTUzNSBmDQowMDAwMDAzNDI5IDAwMDAwIG4NCjAwMDAwMDM2NTggMDAwMDAgbg0KdHJhaWxlcg08PA0vU2l6ZSAzDS9JRFs8ZDcwZjQ2YzViYTRmZThiZDQ5YTlkZDA1OTliMGIxNTE+PGQ3MGY0NmM1YmE0ZmU4YmQ0OWE5ZGQwNTk5YjBiMTUxPl0NPj4Nc3RhcnR4cmVmDTE3Mw0lJUVPRg0=".toBase64DecodedByteArray()

    documentRepository.save(
      Document(
        documentId,
        matchedRecallId,
        documentCategory,
        fileName,
        version,
        details,
        OffsetDateTime.now(),
        userIdOnes
      )
    )

    s3Service.uploadFile(documentId, documentBytes)
  }

  fun `a missing document record and email document exist`(
    documentCategories: Set<DocumentCategory>,
    version: Int,
    details: String
  ) {

    val emailId = DocumentId(UUID.randomUUID())
    `a document exists`(emailId, MISSING_DOCUMENTS_EMAIL, FileName("MDR email.msg"), version, null)

    missingDocumentsRecordRepository.save(
      MissingDocumentsRecord(
        MissingDocumentsRecordId(UUID.randomUUID()),
        matchedRecallId,
        documentCategories,
        emailId,
        details,
        version,
        userIdOnes,
        OffsetDateTime.now()
      )
    )
  }

  fun `a part B record and related documents exist`(
    version: Int,
    details: String
  ) {

    val documents = mapOf(
      PART_B_RISK_REPORT to DocumentId(UUID.randomUUID()),
      PART_B_EMAIL_FROM_PROBATION to DocumentId(UUID.randomUUID()),
      OASYS_RISK_ASSESSMENT to DocumentId(UUID.randomUUID()),
    )

    documents.forEach {
      `a document exists`(it.value, it.key, FileName("MDR email.msg"), 1, null)
    }

    partBRecordRepository.save(
      PartBRecord(
        PartBRecordId(UUID.randomUUID()),
        matchedRecallId,
        details,
        LocalDate.now(),
        documents[PART_B_RISK_REPORT]!!,
        documents[PART_B_EMAIL_FROM_PROBATION]!!,
        documents[OASYS_RISK_ASSESSMENT],
        version,
        userIdOnes,
        OffsetDateTime.now()
      )
    )
  }

  fun `a note exists`(
    index: Int,
    noteId: NoteId = ::NoteId.random(),
    documentId: DocumentId = ::DocumentId.random()
  ) {

    `a document exists`(documentId, NOTE_DOCUMENT, FileName("recall-note.doc"), null, null)

    // Always filling the optional `documentId` here as PACTs such as "Given a recall with notes exists ... a get recall
    // request ... returns a response which ... has a matching body" become fragile when optional fields are missing.
    // See e.g. https://docs.pact.io/faq/#:~:text=Why%20is%20there%20no%20support,best%20tool%20for%20your%20situation.
    noteRepository.save(
      Note(
        noteId,
        matchedRecallId,
        "Some note subject",
        "Some note detail text",
        index,
        documentId,
        userIdOnes,
        OffsetDateTime.now(),
      )
    )
  }

  fun `a last known address exists`(
    index: Int,
    source: AddressSource,
    lastKnownAddressId: LastKnownAddressId = ::LastKnownAddressId.random()
  ) {

    lastKnownAddressRepository.save(
      LastKnownAddress(
        lastKnownAddressId,
        matchedRecallId,
        "Highwood Cottage",
        "43 Blandford Road",
        "Wareham",
        "DT3 7HU",
        source,
        index,
        userIdOnes,
        OffsetDateTime.now()
      )
    )
  }

  fun `an open rescind record exists`() {
    `a document exists`(matchedDocumentId, RESCIND_REQUEST_EMAIL, FileName("email.msg"), null, null)
    rescindRecordRepository.save(
      RescindRecord(
        matchedRescindRecordId,
        matchedRecallId,
        1,
        userIdOnes,
        OffsetDateTime.now(),
        matchedDocumentId,
        "Some info about the rescind",
        LocalDate.now()
      )
    )
  }
}

@Suppress("unused")
@PactFilter(value = [".*unauthorized.*"])
class ManagerRecallsUiUnauthorizedPactTest : ManagerRecallsUiPactTestBase() {
  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTest(pactContext: PactVerificationContext, request: HttpRequest) {
    pactContext.verifyInteraction()
  }

  @State("an unauthorized user accessToken")
  fun `an unauthorized user accessToken`() {
  }
}

/**
 * This test is used to verify the contract between manage-recalls-api and manage-recalls-ui.
 * Verification of PACTs in this project can be run by e.g.: `./gradlew verifyPactAndPublish`
 * It defaults to verify the pact published with the 'main' tag.
 * To override this for verifying a different pact you can either specify the tag of a published pact file:
 * @PactBroker(
 *   consumerVersionSelectors = [ VersionSelector(tag = "pact") ]
 * )
 * Or specify a local pact file by removing @PactBroker and replacing with:
 * e.g. @PactFolder("../manage-recalls-ui/pact/pacts")
 * (if using @PactFolder, ensure it's been imported in this file with `import au.com.dius.pact.provider.junitsupport.loader.PactFolder`)
 */
@ExtendWith(SpringExtension::class)
@VerificationReports(value = ["console"])
@Provider("manage-recalls-api")
@Consumer("manage-recalls-ui")
@PactBroker
abstract class ManagerRecallsUiPactTestBase : ComponentTestBase() {
  @LocalServerPort
  private val port = 0

  @BeforeEach
  fun before(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }
}
