package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedInstance
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DossierContextTest {

  private val nomsNumber = NomsNumber("AA1234A")
  private val bookingNumber = "B1234"
  private val licenceConditionsBreached = "(i) breach one\n(ii) breach two"
  private val firstName = FirstName("Barrie")
  private val lastName = LastName("Badger")
  private val recallLength = RecallLength.TWENTY_EIGHT_DAYS
  private val currentPrisonName = PrisonName("Wormwood Rubs")
  private val recall = Recall(
    ::RecallId.random(),
    nomsNumber, ::UserId.random(), OffsetDateTime.now(), firstName, null, lastName, CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
    recallLength = recallLength,
    bookingNumber = bookingNumber,
    licenceConditionsBreached = licenceConditionsBreached
  )

  @Test
  fun `create ReasonsForRecallContext with all values correctly populated`() {
    val underTest = DossierContext(recall, currentPrisonName, false, 3, STANDARD)

    val result = underTest.getReasonsForRecallContext()

    assertThat(
      result,
      equalTo(
        ReasonsForRecallContext(
          PersonName(firstName, lastName = lastName).firstAndLastName(),
          bookingNumber,
          nomsNumber,
          licenceConditionsBreached,
          "$lastName $firstName"
        )
      )
    )
  }

  @Test
  fun `create TableOfContentsContext with all values correctly populated`() {
    val underTest = DossierContext(recall, currentPrisonName, false, 3, FIXED)

    val result = underTest.getTableOfContentsContext()

    assertThat(
      result,
      equalTo(
        TableOfContentsContext(
          PersonName(firstName, lastName = lastName).firstAndLastName(),
          RecallLengthDescription(recallLength),
          currentPrisonName,
          bookingNumber,
          3,
          FIXED
        )
      )
    )
  }

  @ParameterizedTest(name = "For Welsh LDU or Welsh current prison then includeWelsh is true")
  @MethodSource("sampleOfLDUsWithWelshOrNot")
  fun `Welsh LDU or Welsh current prison return includeWelsh as true whilst neither returns false`(
    ldu: LocalDeliveryUnit,
    currentPrisonIsWelsh: Boolean,
    expected: Boolean
  ) {
    val probationInfo = fullyPopulatedInstance<ProbationInfo>()
    val underTest = DossierContext(
      recall.copy(probationInfo = probationInfo.copy(localDeliveryUnit = ldu)),
      currentPrisonName,
      currentPrisonIsWelsh,
      2,
      FIXED
    )

    assertThat(underTest.includeWelsh(), equalTo(expected))
  }

  private fun sampleOfLDUsWithWelshOrNot(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(LocalDeliveryUnit.PS_HOUNSLOW, false, false, "false for non-Welsh LDU and current prison"),
      Arguments.of(LocalDeliveryUnit.PS_NORTH_WALES, false, true, "true for Welsh LDU and non-Welsh current prison"),
      Arguments.of(LocalDeliveryUnit.PS_DYFED_POWYS, true, true, "true for Welsh LDU and current prison"),
      Arguments.of(LocalDeliveryUnit.PS_NORTH_DURHAM, true, true, "true for non-Welsh LDU and Welsh current prison")
    )
  }
}
