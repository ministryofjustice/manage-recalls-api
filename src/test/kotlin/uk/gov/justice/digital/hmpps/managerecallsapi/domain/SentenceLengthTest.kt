package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SentenceLengthTest {

  @ParameterizedTest(name = "recall length is {0} for sentence length y={1}, m={2}, d={3}")
  @MethodSource("parameterArrays")
  fun `recall length is 14 days if sentence length less than one year for Fixed Term recall`(
    recallLength: RecallLength,
    sentenceYears: Int,
    sentenceMonths: Int,
    sentenceDays: Int,
    prettyPrint: String
  ) {
    val sentenceLength = SentenceLength(sentenceYears, sentenceMonths, sentenceDays)
    assertThat(
      recallLength,
      equalTo(
        SentencingInfo(
          LocalDate.now(),
          LocalDate.now(),
          LocalDate.now(),
          CourtId("HIGHCT"),
          "offence",
          sentenceLength
        ).calculateRecallLength(RecallType.FIXED)
      )
    )
    assertThat(sentenceLength.toString(), equalTo(prettyPrint))
  }

  private fun parameterArrays(): Stream<Arguments>? =
    Stream.of(
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 1, 0, 0, "1 years 0 months 0 days"),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 0, 12, 0, "0 years 12 months 0 days"),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 0, 0, 366, "0 years 0 months 366 days"),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 1, 1, 1, "1 years 1 months 1 days"),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 11, 0, "0 years 11 months 0 days"),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 0, 365, "0 years 0 months 365 days"),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 11, 30, "0 years 11 months 30 days"),
    )
}
