package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.OffsetDateTime

class DossierContextTest {

  private val nomsNumber = NomsNumber("AA1234A")
  private val bookingNumber = "B1234"
  private val licenceConditionsBreached = "(i) breach one\n(ii) breach two"
  private val firstName = "Bertie"
  private val lastName = "Badger"
  private val recall = Recall(
    ::RecallId.random(),
    nomsNumber, ::UserId.random(), OffsetDateTime.now(),
    OffsetDateTime.now(),
    bookingNumber = bookingNumber,
    licenceConditionsBreached = licenceConditionsBreached
  )
  private val prisoner = Prisoner(firstName = firstName, lastName = lastName)

  @Test
  fun `create ReasonsForRecallContext with all values populated`() {
    val underTest = DossierContext(recall, prisoner, PrisonName("N/A"))

    val result = underTest.getReasonsForRecallContext()

    assertThat(
      result,
      equalTo(
        ReasonsForRecallContext(
          PersonName(firstName, lastName),
          bookingNumber,
          nomsNumber,
          licenceConditionsBreached
        )
      )
    )
  }
}
