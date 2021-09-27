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
          val t = Phrase(numberToPrintOnPage.toString(), Liberation.SERIF.create(10))
          val xt: Float = pdfReader.getPageSize(pageNumber).width / 2
          val yt: Float = pdfReader.getPageSize(pageNumber).getBottom(20f)
          ColumnText.showTextAligned(
            pdfStamper.getOverContent(pageNumber), Element.ALIGN_CENTER,
            t, xt, yt, 0f
          )
        }
        pdfStamper.close()
      }
      output.toByteArray()
    }
}
