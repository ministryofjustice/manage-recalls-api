package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms

class SearchRecallsComponentTest : ComponentTestBase() {

  @Test
  fun `search recalls by nomsNumber`() {
    val nomsNumberToSearch = randomNoms()
    val recall1 = Recall(::RecallId.random(), randomNoms())
    val recall2 = Recall(::RecallId.random(), nomsNumberToSearch)
    val recall3 = Recall(::RecallId.random(), nomsNumberToSearch)
    val recall4 = Recall(::RecallId.random(), randomNoms())
    recallRepository.saveAll(listOf(recall1, recall2, recall3, recall4))

    val response = authenticatedClient.searchRecalls(RecallSearchRequest(nomsNumberToSearch))

    assertThat(
      response, List<RecallResponse>::equals,
      listOf(
        RecallResponse(recall2.recallId(), nomsNumberToSearch),
        RecallResponse(recall3.recallId(), nomsNumberToSearch)
      )
    )
  }
}
