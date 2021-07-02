package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository

class RecallsControllerTest {
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = RecallsController(recallRepository)

  private val nomsNumber = "A1234AA"
  private val recallRequest = BookRecallRequest(nomsNumber)

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall()
    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    assertThat(results.body, equalTo(BookRecallResponse(recall.id, nomsNumber)))
  }

  @Test
  fun `gets all recalls`() {
    val recall = recallRequest.toRecall()
    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(recall)))
  }
}
