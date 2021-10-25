package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class SearchRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `search recalls by nomsNumber`() {
    val nomsNumberToSearch = randomNoms()
    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))
    val recall1 = Recall(::RecallId.random(), randomNoms(), now, now)
    val recall2 = Recall(::RecallId.random(), nomsNumberToSearch, now, now)
    val recall3 = Recall(::RecallId.random(), nomsNumberToSearch, now, now)
    val recall4 = Recall(::RecallId.random(), randomNoms(), now, now)
    recallRepository.saveAll(listOf(recall1, recall2, recall3, recall4))

    val response = authenticatedClient.searchRecalls(RecallSearchRequest(nomsNumberToSearch))

    assertThat(
      response, List<RecallResponse>::equals,
      listOf(
        RecallResponse(recall2.recallId(), nomsNumberToSearch, now, now),
        RecallResponse(recall3.recallId(), nomsNumberToSearch, now, now)
      )
    )
  }
}
