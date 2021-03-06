package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.FixedTermRecallInformationLeafletEnglish
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.readText
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import java.time.LocalDate

class GenerateDossierComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedMergedPdf = ClassPathResource("/document/recall-notification.pdf").file.readBytes()

  @Test
  fun `can generate the dossier sending the correct documents to gotenberg`() {
    val revocationOrderFile = ClassPathResource("/document/revocation-order.pdf").file
    val tableOfContentsFile = ClassPathResource("/document/table-of-contents.pdf").file
    val reasonsForRecallContentsFile = ClassPathResource("/document/reasons-for-recall.pdf").file // TODO use rep. pdf

    gotenbergMockServer.stubGenerateRevocationOrder(revocationOrderFile.readBytes(), firstName)
    gotenbergMockServer.stubGenerateTableOfContents(tableOfContentsFile.readBytes())
    gotenbergMockServer.stubGenerateReasonsForRecall(reasonsForRecallContentsFile.readBytes())

    gotenbergMockServer.stubMergePdfs(
      expectedMergedPdf,
      tableOfContentsFile.readText(),
      FixedTermRecallInformationLeafletEnglish.readText(),
      ClassPathResource("/document/licence.pdf").file.readText(),
      ClassPathResource("/document/part_a.pdf").file.readText(),
      revocationOrderFile.readText(),
    )

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName(firstName),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    authenticatedClient.updateRecommendedRecallType(recall.recallId, FIXED)
    authenticatedClient.updateRecall(
      recall.recallId,
      UpdateRecallRequest(
        authorisingAssistantChiefOfficer = FullName("not empty"),
        bookingNumber = BookingNumber("bookingNumber"),
        currentPrison = PrisonId("MWI"),
        inCustodyAtBooking = true,
        indexOffence = "Badgering",
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        lastReleasePrison = PrisonId("CFI"),
        licenceConditionsBreached = "he was a very naughty boy",
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        licenceNameCategory = NameFormatCategory.FIRST_LAST,
        localDeliveryUnit = LocalDeliveryUnit.PS_SWINDON_AND_WILTSHIRE,
        localPoliceForceId = PoliceForceId("avon-and-somerset"),
        probationOfficerEmail = Email("not empty"),
        probationOfficerName = FullName("not empty"),
        probationOfficerPhoneNumber = PhoneNumber("not empty"),
        sentenceDate = LocalDate.of(2012, 5, 17),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(10, 1, 5),
        sentencingCourt = CourtId("CARLCT"),
      )
    )
    expectNoVirusesWillBeFound()

    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(LICENCE, base64EncodedFileContents("/document/licence.pdf"), FileName("filename.pdf"))
    )
    authenticatedClient.uploadDocument(
      recall.recallId,
      UploadDocumentRequest(PART_A_RECALL_REPORT, base64EncodedFileContents("/document/part_a.pdf"), FileName("part_a.pdf"))
    )

    authenticatedClient.generateDocument(recall.recallId, REVOCATION_ORDER, FileName("REVOCATION_ORDER.pdf"))

    val dossierId = authenticatedClient.generateDocument(recall.recallId, DOSSIER, FileName("DOSSIER.pdf"))
    val dossier = authenticatedClient.getDocument(recall.recallId, dossierId.documentId)

    assertThat(Pdf(dossier.content), hasNumberOfPages(equalTo(3)))
  }
}
