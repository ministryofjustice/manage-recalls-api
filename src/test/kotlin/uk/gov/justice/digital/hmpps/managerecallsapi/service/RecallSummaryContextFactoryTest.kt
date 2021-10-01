package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class RecallSummaryContextFactoryTest {
  private val underTest = RecallSummaryContextFactory()

  @Test
  fun `context contains required recall and prisoner information`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = "AAA"
    val lastReleasePrison = "ZZZ"
    val prisoner = mockk<Prisoner>()
    val assessorUserId = ::UserId.random()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrison, lastReleasePrison = lastReleasePrison)
    val currentPrisonName = "Current Prison Name"
    val lastReleasePrisonName = "Last Release Prison Name"
    val userDetails = UserDetails(
      assessorUserId,
      FirstName("Bertie"),
      LastName("Badger"),
      "",
      Email("b@b.com"),
      PhoneNumber("09876543210")
    )

    val recallNotificationContext =
      RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName)
    val result = underTest.createRecallSummaryContext(recallNotificationContext).block()!!

    assertThat(
      result,
      equalTo(
        RecallSummaryContext(recall, prisoner, lastReleasePrisonName, currentPrisonName, userDetails)
      )
    )
  }
}
