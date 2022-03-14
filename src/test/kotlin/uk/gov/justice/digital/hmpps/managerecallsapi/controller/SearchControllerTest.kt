package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.LocalDate
import java.util.UUID

class SearchControllerTest {

  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = SearchController(prisonerOffenderSearchClient)

  private val nomsNumber = NomsNumber("A1234AA")

  @Test
  fun `prisonerSearch returns prisoner results`() {
    val prisoner1 = prisoner("A1234AA", "Jim", "Smith", "Norman", LocalDate.of(1994, 10, 15))
    every { prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber) } returns Mono.just(prisoner1)

    val results = underTest.prisonerByNomsNumber(nomsNumber)

    StepVerifier
      .create(results)
      .assertNext {
        assertThat(
          it,
          equalTo(
            prisoner1.toApiPrisoner(),
          )
        )
      }
      .verifyComplete()
  }

  private fun Prisoner.toApiPrisoner() =
    SearchController.Api.Prisoner(
      this.firstName,
      this.middleNames,
      this.lastName,
      this.dateOfBirth,
      this.gender,
      this.prisonerNumber,
      this.pncNumber,
      this.croNumber
    )

  private fun prisoner(
    nomsNumber: String?,
    firstName: String?,
    lastName: String?,
    middleNames: String?,
    dateOfBirth: LocalDate?
  ) = Prisoner(
    prisonerNumber = nomsNumber,
    pncNumber = UUID.randomUUID().toString(),
    croNumber = UUID.randomUUID().toString(),
    firstName = firstName,
    lastName = lastName,
    middleNames = middleNames,
    dateOfBirth = dateOfBirth,
    gender = UUID.randomUUID().toString(),
  )
}
