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

  fun numberPages(pdfContent: ByteArray): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val pdfReader = PdfReader(pdfContent)
    val pdfStamper = PdfStamper(pdfReader, outputStream)
    for (i in 1..pdfReader.numberOfPages) {
      val t = Phrase(i.toString(), Liberation.SERIF.create(10))
      val xt: Float = pdfReader.getPageSize(i).width / 2
      val yt: Float = pdfReader.getPageSize(i).getBottom(20f)
      ColumnText.showTextAligned(
        pdfStamper.getOverContent(i), Element.ALIGN_CENTER,
        t, xt, yt, 0f
      )
    }
    pdfStamper.close()
    pdfReader.close()
    outputStream.close()
    return outputStream.toByteArray()
  }
}
