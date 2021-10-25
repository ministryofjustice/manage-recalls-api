package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class GetRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `get all recalls`() {
    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))
    val recall1 = Recall(::RecallId.random(), NomsNumber("123456"), now, now)
    val recall2 = Recall(::RecallId.random(), NomsNumber("987654"), now, now)
    recallRepository.save(recall1)
    recallRepository.save(recall2)

    val response = authenticatedClient.getAllRecalls()

    assertThat(
      response, List<RecallResponse>::containsAll,
      listOf(
        RecallResponse(recall1.recallId(), recall1.nomsNumber, now, now),
        RecallResponse(recall2.recallId(), recall2.nomsNumber, now, now)
      )
    )
  }
}
