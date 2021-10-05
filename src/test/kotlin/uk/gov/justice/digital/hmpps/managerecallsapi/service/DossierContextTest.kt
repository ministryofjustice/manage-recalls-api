package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class DossierContextTest {

  private val nomsNumber = NomsNumber("AA1234A")
  private val bookingNumber = "B1234"
  private val licenceConditionsBreached = "(i) breach one\n(ii) breach two"
  private val firstName = "Bertie"
  private val middleNames = "Basset"
  private val lastName = "Badger"
  private val recall = Recall(
    ::RecallId.random(),
    nomsNumber,
    bookingNumber = bookingNumber,
    licenceConditionsBreached = licenceConditionsBreached
  )
  private val prisoner = Prisoner(firstName = firstName, middleNames = middleNames, lastName = lastName)

  @Test
  fun `create ReasonsForRecallContext with all values populated`() {
    val underTest = DossierContext(recall, prisoner, PrisonName("N/A"))

    val result = underTest.getReasonsForRecallContext()

    assertThat(
      result,
      equalTo(
        ReasonsForRecallContext(
          FirstAndMiddleNames(FirstName(firstName), MiddleNames(middleNames)),
          LastName(lastName),
          bookingNumber,
          nomsNumber,
          licenceConditionsBreached
        )
      )
    )
  }

  @Test
  fun `create ReasonsForRecallContext without prisoner middles names`() {
    val prisonerWithoutMiddlesNames = prisoner.copy(middleNames = null)
    val underTest = DossierContext(recall, prisonerWithoutMiddlesNames, PrisonName("N/A"))

    val result = underTest.getReasonsForRecallContext()

    assertThat(
      result,
      equalTo(
        ReasonsForRecallContext(
          FirstAndMiddleNames(FirstName(firstName)),
          LastName(lastName),
          bookingNumber,
          nomsNumber,
          licenceConditionsBreached
        )
      )
    )
  }
}
