package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddressRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomIndex
import java.util.UUID

class LastKnownAddressControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val lastKnownAddressRepository = mockk<LastKnownAddressRepository>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val underTest = LastKnownAddressController(
    recallRepository,
    lastKnownAddressRepository,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `stores first lastKnownAddress with index 1, valid ids and passed in address properties`() {
    val recall = mockk<Recall>()
    val savedLastKnownAddress = slot<LastKnownAddress>()
    val lastKnownAddress = mockk<LastKnownAddress>()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val lastKnownAddressId = ::LastKnownAddressId.random()

    val line1 = "address line 1"
    val line2 = "address line 2"
    val town = "some town"
    val postcode = "some postcode"
    val source = AddressSource.LOOKUP

    every { recall.lastKnownAddresses } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { lastKnownAddressRepository.save(capture(savedLastKnownAddress)) } returns lastKnownAddress
    every { lastKnownAddress.id() } returns lastKnownAddressId

    val request = CreateLastKnownAddressRequest(
      line1, line2, town, postcode, source
    )

    val response = underTest.createLastKnownAddress(recallId, request, bearerToken)

    assertThat(savedLastKnownAddress.captured.index, equalTo(1))
    assertThat(UUID.fromString(savedLastKnownAddress.captured.id.toString()), present())
    assertThat(savedLastKnownAddress.captured.recallId, equalTo(recallId.value))
    assertThat(savedLastKnownAddress.captured.line1, equalTo(line1))
    assertThat(savedLastKnownAddress.captured.line2, equalTo(line2))
    assertThat(savedLastKnownAddress.captured.town, equalTo(town))
    assertThat(savedLastKnownAddress.captured.postcode, equalTo(postcode))
    assertThat(savedLastKnownAddress.captured.source, equalTo(source))
    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.body,
      equalTo(
        lastKnownAddressId
      )
    )
  }

  @Test
  fun `stores next lastKnownAddress with previous maximum index plus 1 and passed in address properties`() {
    val recall = mockk<Recall>()
    val savedLastKnownAddress = slot<LastKnownAddress>()
    val address = mockk<LastKnownAddress>()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val lastKnownAddressId = ::LastKnownAddressId.random()

    val line1 = "address line 1"
    val line2 = null
    val town = "some town"
    val postcode = null
    val source = AddressSource.MANUAL

    val preExistingLastKnownAddress = mockk<LastKnownAddress>()
    every { recall.lastKnownAddresses } returns setOf(preExistingLastKnownAddress)
    val previousMaxIndex = randomIndex()
    every { preExistingLastKnownAddress.index } returns previousMaxIndex
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { lastKnownAddressRepository.save(capture(savedLastKnownAddress)) } returns address
    every { address.id() } returns lastKnownAddressId

    val request = CreateLastKnownAddressRequest(
      line1, line2, town, postcode, source
    )

    val response = underTest.createLastKnownAddress(recallId, request, bearerToken)

    assertThat(savedLastKnownAddress.captured.index, equalTo(previousMaxIndex + 1))
    assertThat(savedLastKnownAddress.captured.line1, equalTo(line1))
    assertThat(savedLastKnownAddress.captured.line2, equalTo(null))
    assertThat(savedLastKnownAddress.captured.town, equalTo(town))
    assertThat(savedLastKnownAddress.captured.postcode, equalTo(null))
    assertThat(savedLastKnownAddress.captured.source, equalTo(source))
    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.body,
      equalTo(
        lastKnownAddressId
      )
    )
  }

  @Test
  fun `deletes address`() {
    val lastKnownAddressId = ::LastKnownAddressId.random()
    val recallId = ::RecallId.random()
    every { lastKnownAddressRepository.deleteByRecallIdAndLastKnownAddressId(recallId, lastKnownAddressId) } just Runs

    underTest.deleteAddress(recallId, lastKnownAddressId)

    verify { lastKnownAddressRepository.deleteByRecallIdAndLastKnownAddressId(recallId, lastKnownAddressId) }
  }
}
