package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateLastKnownAddressRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import javax.transaction.Transactional

class LastKnownAddressComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger"))

  @Test
  fun `create and retrieve one LastKnownAddress for a recall`() {
    val originalRecall = authenticatedClient.bookRecall(bookRecallRequest)
    val createLastKnownAddressRequest = CreateLastKnownAddressRequest(
      originalRecall.recallId,
      "address line 1",
      "some line 2",
      "some town",
      "the postcode",
      AddressSource.MANUAL
    )

    val lastKnownAddressId = authenticatedClient.addLastKnownAddress(createLastKnownAddressRequest, CREATED, LastKnownAddressId::class.java)
    assertThat(lastKnownAddressId, present())

    val updatedRecall = authenticatedClient.getRecall(originalRecall.recallId)
    assertThat(originalRecall.lastKnownAddresses, isEmpty)

    val lastKnownAddresses = updatedRecall.lastKnownAddresses
    assertThat(lastKnownAddresses.size, equalTo(1))
    assertThat(lastKnownAddresses.first().line1, equalTo("address line 1"))
    assertThat(lastKnownAddresses.first().line2, equalTo("some line 2"))
    assertThat(lastKnownAddresses.first().town, equalTo("some town"))
    assertThat(lastKnownAddresses.first().postcode, equalTo("the postcode"))
    assertThat(lastKnownAddresses.first().index, equalTo(1))
    assertThat(lastKnownAddresses.first().source.toString(), equalTo("MANUAL"))
    assertThat(lastKnownAddresses.first().lastKnownAddressId.toString(), equalTo(lastKnownAddressId.value.toString()))
  }

  @Test
  fun `adding 2 LastKnownAddresses for the same recall, optional properties can be null, both will be returned on the recall with incrementing index`() {
    val originalRecall = authenticatedClient.bookRecall(bookRecallRequest)
    val firstLastKnownAddressRequest = CreateLastKnownAddressRequest(
      originalRecall.recallId,
      "address line 1",
      "some line 2",
      "first town",
      "the postcode",
      AddressSource.LOOKUP
    )

    val lastKnownAddressId1 = authenticatedClient.addLastKnownAddress(firstLastKnownAddressRequest, CREATED, LastKnownAddressId::class.java)
    assertThat(lastKnownAddressId1, present())

    val secondLastKnownAddressRequest = firstLastKnownAddressRequest.copy(
      recallId = null,
      line2 = null,
      town = "second town",
      postcode = null,
      source = AddressSource.MANUAL
    )
    val lastKnownAddressId2 = authenticatedClient.addLastKnownAddress(originalRecall.recallId, secondLastKnownAddressRequest, CREATED, LastKnownAddressId::class.java)
    assertThat(lastKnownAddressId2, present())
    assertThat(lastKnownAddressId1, !equalTo(lastKnownAddressId2))

    assertThat(originalRecall.lastKnownAddresses, isEmpty)

    val updatedRecall = authenticatedClient.getRecall(originalRecall.recallId)
    val lastKnownAddresses = updatedRecall.lastKnownAddresses
    assertThat(lastKnownAddresses.size, equalTo(2))

    assertThat(lastKnownAddresses[0].index, !equalTo(lastKnownAddresses[1].index))
    // There is no contract for the order of LastKnownAddresses but the index must be one-based incrementing positive integers, hence 1, 2, etc.
    val lastKnownAddressIndex1 = lastKnownAddresses[lastKnownAddresses.indexOfFirst { it.index == 1 }]
    val lastKnownAddressIndex2 = lastKnownAddresses[lastKnownAddresses.indexOfFirst { it.index == 2 }]
    assertThat(lastKnownAddressIndex1.lastKnownAddressId, equalTo(lastKnownAddressId1))
    assertThat(lastKnownAddressIndex2.lastKnownAddressId, equalTo(lastKnownAddressId2))
    assertThat(lastKnownAddressIndex1.town, equalTo("first town"))
    assertThat(lastKnownAddressIndex2.town, equalTo("second town"))
    assertThat(lastKnownAddressIndex1.source.toString(), equalTo("LOOKUP"))
    assertThat(lastKnownAddressIndex2.source.toString(), equalTo("MANUAL"))
    assertThat(lastKnownAddressIndex2.line2, equalTo(null))
    assertThat(lastKnownAddressIndex2.postcode, equalTo(null))
  }

  @Test
  fun `add a LastKnownAddress with an incorrect recallId returns NOT_FOUND with message`() {
    val notFoundRecallId = ::RecallId.random()
    val createLastKnownAddressRequest = CreateLastKnownAddressRequest(
      null,
      "address line 1",
      "some line 2",
      "some town",
      "the postcode",
      AddressSource.MANUAL
    )

    val expectedStatus = HttpStatus.NOT_FOUND
    val response = authenticatedClient.addLastKnownAddress(notFoundRecallId, createLastKnownAddressRequest, expectedStatus, ErrorResponse::class.java)

    assertThat(response, present())
    assertThat(response, equalTo(ErrorResponse(expectedStatus, "RecallNotFoundException(recallId=$notFoundRecallId)")))
  }

  @Test
  @Transactional
  fun `can delete a LastKnownAddress`() {
    val originalRecall = authenticatedClient.bookRecall(bookRecallRequest)
    val firstLastKnownAddressRequest = CreateLastKnownAddressRequest(
      null,
      "address line 1",
      "some line 2",
      "first town",
      "the postcode",
      AddressSource.LOOKUP
    )

    val lastKnownAddressId = authenticatedClient.addLastKnownAddress(originalRecall.recallId, firstLastKnownAddressRequest, CREATED, LastKnownAddressId::class.java)

    authenticatedClient.deleteLastKnownAddress(originalRecall.recallId, lastKnownAddressId)
  }

  @Test
  fun `error thrown trying to delete a LastKnownAddress that doesnt exist`() {
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    authenticatedClient.deleteLastKnownAddress(recall.recallId, ::LastKnownAddressId.random(), expectedStatus = HttpStatus.NOT_FOUND)
  }
}
