package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

@Component
class RevocationOrderGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
  @Autowired private val clock: Clock
) {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", ENGLISH)

  fun generateHtml(prisoner: Prisoner, recall: Recall): String =
    Context().apply {
      val firstAndMiddleNames = String.format("%s %s", prisoner.firstName ?: "", prisoner.middleNames ?: "").trim()
      val todaysDate = LocalDate.now(clock).format(dateTimeFormatter)
      val lastReleaseDate = recall.lastReleaseDate?.format(dateTimeFormatter)
      setVariable("firstNames", firstAndMiddleNames)
      setVariable("lastName", prisoner.lastName)
      setVariable("dateOfBirth", prisoner.dateOfBirth)
      setVariable("prisonNumber", prisoner.bookNumber)
      setVariable("croNumber", prisoner.croNumber)
      setVariable("licenseRevocationDate", todaysDate)
      setVariable("lastReleaseDate", lastReleaseDate)
    }.let {
      templateEngine.process("revocation-order", it)
    }
}
