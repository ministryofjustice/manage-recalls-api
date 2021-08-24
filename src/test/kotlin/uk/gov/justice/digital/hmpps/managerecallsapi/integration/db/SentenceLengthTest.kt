package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SentenceLengthTest {

  @ParameterizedTest(name = "recall length is {0} for sentence length y={1}, m={2}, d={3}")
  @MethodSource("parameterArrays")
  fun `recall length is 14 days if sentence length less than one year`(
    recallLength: RecallLength,
    sentenceYears: Int,
    sentenceMonths: Int,
    sentenceDays: Int
  ) {
    assertThat(
      recallLength,
      equalTo(
        SentencingInfo(
          LocalDate.now(),
          LocalDate.now(),
          LocalDate.now(),
          "court",
          "offence",
          SentenceLength(sentenceYears, sentenceMonths, sentenceDays)
        ).calculateRecallLength()
      )
    )
  }

  fun parameterArrays(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 1, 0, 0),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 0, 12, 0),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 0, 0, 366),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, 1, 1, 1),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 11, 0),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 0, 365),
      Arguments.of(RecallLength.FOURTEEN_DAYS, 0, 11, 30),
    )
  }
}
