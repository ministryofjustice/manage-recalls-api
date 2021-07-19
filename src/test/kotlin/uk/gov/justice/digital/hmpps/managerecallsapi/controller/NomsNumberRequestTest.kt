package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import org.hibernate.validator.internal.engine.path.PathImpl
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.isSingleItemMatching
import java.util.stream.Stream
import javax.validation.Validation

@TestInstance(PER_CLASS)
class NomsNumberRequestTest {

  private val validatorFactory = Validation.buildDefaultValidatorFactory()
  private val validator = validatorFactory.validator

  @Suppress("unused")
  private fun invalidNomsNumberRequests() = Stream.of(
    SearchRequest(" "),
    BookRecallRequest(NomsNumber(" ")),
  )

  @ParameterizedTest
  @MethodSource("invalidNomsNumberRequests")
  fun `request is invalid if it has a blank nomsNumber`(invalidRequest: Any) {
    val violations = validator.validate(invalidRequest)

    assertThat(
      violations,
      isSingleItemMatching(
        allOf(
          has("propertyPath", { it.propertyPath }, equalTo(PathImpl.createPathFromString("nomsNumber"))),
          has("message", { it.message }, equalTo("must not be blank"))
        )
      )
    )
  }

  @Suppress("unused")
  private fun validNomsNumberRequests() = Stream.of(
    SearchRequest("noms"),
    BookRecallRequest(NomsNumber("noms")),
  )

  @ParameterizedTest
  @MethodSource("validNomsNumberRequests")
  fun `request is valid if it has a non-empty nomsNumber`(validRequest: Any) {
    val violations = validator.validate(validRequest)

    assertThat(violations, isEmpty)
  }
}
