package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import org.hibernate.validator.internal.engine.path.PathImpl
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.isSingleItemMatching
import javax.validation.Validation

class SearchRequestTest {

  private val validatorFactory = Validation.buildDefaultValidatorFactory()
  private val validator = validatorFactory.validator

  @Test
  fun `searchRequest is invalid if it has a blank nomsNumber`() {
    val invalidSearchRequest = SearchRequest(" ")
    val violations = validator.validate(invalidSearchRequest)

    assertThat(violations, isSingleItemMatching(
        allOf(
          has("propertyPath", { it.propertyPath }, equalTo(PathImpl.createPathFromString("nomsNumber"))),
          has("message", { it.message }, equalTo("must not be blank"))
        )
      )
    )
  }

  @Test
  fun `searchRequest is valid if it has a non-empty nomsNumber`() {
    val invalidSearchRequest = SearchRequest("noms")
    val violations = validator.validate(invalidSearchRequest)

    assertThat(violations, isEmpty)
  }
}
