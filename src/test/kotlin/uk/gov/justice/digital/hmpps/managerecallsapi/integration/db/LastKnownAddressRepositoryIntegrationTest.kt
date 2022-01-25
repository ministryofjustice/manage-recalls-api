package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime
import javax.transaction.Transactional

class LastKnownAddressRepositoryIntegrationTest : IntegrationTestBase() {

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush one then a second last known address with valid index values for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val lastKnownAddressId1 = ::LastKnownAddressId.random()
    val lastKnownAddress1 = lastKnownAddress(lastKnownAddressId1, 1)

    lastKnownAddressRepository.saveAndFlush(lastKnownAddress1)
    val retrieved1 = lastKnownAddressRepository.getById(lastKnownAddressId1.value)

    assertThat(retrieved1, equalTo(lastKnownAddress1))

    val lastKnownAddressId2 = ::LastKnownAddressId.random()
    val lastKnownAddress2 = lastKnownAddress(lastKnownAddressId2, 2)

    lastKnownAddressRepository.saveAndFlush(lastKnownAddress2)
    val retrieved2 = lastKnownAddressRepository.getById(lastKnownAddressId2.value)

    assertThat(retrieved2, equalTo(lastKnownAddress2))
  }

  @Test
  @Transactional
  fun `cannot save and flush two last known addresses with the same index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val lastKnownAddressId1 = ::LastKnownAddressId.random()
    val lastKnownAddress1 = lastKnownAddress(lastKnownAddressId1, 1)

    lastKnownAddressRepository.saveAndFlush(lastKnownAddress1)
    val retrieved1 = lastKnownAddressRepository.getById(lastKnownAddressId1.value)

    assertThat(retrieved1, equalTo(lastKnownAddress1))

    val lastKnownAddressId2 = ::LastKnownAddressId.random()
    val lastKnownAddress2 = lastKnownAddress(lastKnownAddressId2, 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      lastKnownAddressRepository.saveAndFlush(lastKnownAddress2)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a last known address with an invalid(0) index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val lastKnownAddressId1 = ::LastKnownAddressId.random()
    val lastKnownAddress1 = lastKnownAddress(lastKnownAddressId1, 0)

    val thrown = assertThrows<DataIntegrityViolationException> {
      lastKnownAddressRepository.saveAndFlush(lastKnownAddress1)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a last known address with an invalid(negative) index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val lastKnownAddressId1 = ::LastKnownAddressId.random()
    val lastKnownAddress1 = lastKnownAddress(lastKnownAddressId1, -1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      lastKnownAddressRepository.saveAndFlush(lastKnownAddress1)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  private fun lastKnownAddress(
    lastKnownAddressId: LastKnownAddressId,
    index: Int
  ) = LastKnownAddress(
    lastKnownAddressId,
    recallId,
    "Highwood Cottage, 43 Blandford Road",
    null,
    "Wareham",
    "DT3 7HU",
    AddressSource.MANUAL,
    index,
    createdByUserId,
    OffsetDateTime.now()
  )
}
