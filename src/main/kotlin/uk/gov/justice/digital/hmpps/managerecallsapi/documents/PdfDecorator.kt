package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.lowagie.text.Element
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.ColumnText
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import org.librepdf.openpdf.fonts.Liberation
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PdfDecorator {
  fun numberPages(pdfContent: ByteArray, numberOfPagesToSkip: Int = 0): ByteArray =
    ByteArrayOutputStream().use { output ->
      PdfReader(pdfContent).use { pdfReader ->
        val pdfStamper = PdfStamper(pdfReader, output)

        val firstPageToNumber = numberOfPagesToSkip + 1
        (firstPageToNumber..pdfReader.numberOfPages).forEach { pageNumber ->
          val numberToPrintOnPage = pageNumber - numberOfPagesToSkip
          val text = Phrase("$numberToPrintOnPage", Liberation.SERIF.create(10))
          val xPos: Float = pdfReader.getPageSize(pageNumber).width / 2
          val yPos: Float = pdfReader.getPageSize(pageNumber).getBottom(20f)
          ColumnText.showTextAligned(
            pdfStamper.getOverContent(pageNumber), Element.ALIGN_CENTER,
            text, xPos, yPos, 0f
          )
        }
        pdfStamper.close()
      }
      output.toByteArray()
    }

  fun numberPagesOnRightWithHeaderAndFooter(
    pdfContent: ByteArray,
    numberOfPagesToSkip: Int = 0,
    shouldCountAllPages: Boolean = true,
    headerText: String?,
    firstHeaderPage: Int = 1,
    footerText: String?,
    firstFooterPage: Int = 1
  ): ByteArray =
    ByteArrayOutputStream().use { output ->
      PdfReader(pdfContent).use { pdfReader ->
        val pdfStamper = PdfStamper(pdfReader, output)

        val firstPageToNumber = numberOfPagesToSkip + 1
        (1..pdfReader.numberOfPages).forEach { pageNumber ->
          if (pageNumber >= firstPageToNumber) {
            val numberToPrintOnPage = if (shouldCountAllPages) pageNumber else pageNumber - numberOfPagesToSkip
            val pageNumberPhrase = Phrase("$numberToPrintOnPage", Liberation.SERIF.create(10))
            val xRightPos: Float = pdfReader.getPageSize(pageNumber).width - 40f
            val yPosNumber: Float = pdfReader.getPageSize(pageNumber).getBottom(50f)
            ColumnText.showTextAligned(
              pdfStamper.getOverContent(pageNumber), Element.ALIGN_CENTER,
              pageNumberPhrase, xRightPos, yPosNumber, 0f
            )
          }
          if (pageNumber >= firstHeaderPage) {
            headerText.let { header ->
              val headerPhrase = Phrase("$header", Liberation.SERIF.create(10))
              val xRightPos: Float = pdfReader.getPageSize(pageNumber).width - 20f
              val yPosHeader: Float = pdfReader.getPageSize(pageNumber).getTop(20f)
              ColumnText.showTextAligned(
                pdfStamper.getOverContent(pageNumber), Element.ALIGN_RIGHT,
                headerPhrase, xRightPos, yPosHeader, 0f
              )
            }
          }
          if (pageNumber >= firstFooterPage) {
            footerText.let { footer ->
              val xCentralPos: Float = pdfReader.getPageSize(pageNumber).width / 2
              val footerPhrase = Phrase(footer, Liberation.SERIF_BOLD.create(10))
              val yPosFooter: Float = pdfReader.getPageSize(pageNumber).getBottom(30f)
              ColumnText.showTextAligned(
                pdfStamper.getOverContent(pageNumber), Element.ALIGN_CENTER,
                footerPhrase, xCentralPos, yPosFooter, 0f
              )
            }
          }
        }
        pdfStamper.close()
      }
      output.toByteArray()
    }

  fun centralHeader(
    pdfContent: ByteArray,
    headerText: String,
  ): ByteArray =
    ByteArrayOutputStream().use { output ->
      PdfReader(pdfContent).use { pdfReader ->
        val pdfStamper = PdfStamper(pdfReader, output)

        (1..pdfReader.numberOfPages).forEach { pageNumber ->
          val headerPhrase = Phrase(headerText, Liberation.SERIF_BOLD.create(10))
          val xRightPos: Float = pdfReader.getPageSize(pageNumber).width / 2
          val yPosHeader: Float = pdfReader.getPageSize(pageNumber).getTop(20f)
          ColumnText.showTextAligned(
            pdfStamper.getOverContent(pageNumber), Element.ALIGN_CENTER,
            headerPhrase, xRightPos, yPosHeader, 0f
          )
        }
        pdfStamper.close()
      }
      output.toByteArray()
    }
}
