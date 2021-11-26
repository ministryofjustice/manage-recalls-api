package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
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
  private val firstName = FirstName("Barrie")
  private val lastName = LastName("Badger")
  private val recall = Recall(
    ::RecallId.random(),
    nomsNumber, ::UserId.random(), OffsetDateTime.now(), firstName, null, lastName,
    bookingNumber = bookingNumber,
    licenceConditionsBreached = licenceConditionsBreached
  )
  private val prisoner = Prisoner(firstName = firstName.value, lastName = lastName.value)

  @Test
  fun `create ReasonsForRecallContext with all values populated`() {
    val underTest = DossierContext(recall, PrisonName("N/A"))

    val result = underTest.getReasonsForRecallContext()

    assertThat(
      result,
      equalTo(
        ReasonsForRecallContext(
          PersonName(firstName, lastName = lastName).firstAndLastName(),
          bookingNumber,
          nomsNumber,
          licenceConditionsBreached
        )
      )
    )
  }
}
