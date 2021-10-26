package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.util.UUID

@Configuration
@EnableSwagger2
class SpringFoxConfig {

  @Bean
  fun api(): Docket {
    return Docket(DocumentationType.SWAGGER_2)
      .select()
      .apis(RequestHandlerSelectors.any())
      .paths(PathSelectors.any())
      .build()
      .directModelSubstitute(NomsNumber::class.java, String::class.java)
      .directModelSubstitute(FirstName::class.java, String::class.java)
      .directModelSubstitute(LastName::class.java, String::class.java)
      .directModelSubstitute(Email::class.java, String::class.java)
      .directModelSubstitute(PhoneNumber::class.java, String::class.java)
      .directModelSubstitute(PrisonName::class.java, String::class.java)
      .directModelSubstitute(RecallId::class.java, UUID::class.java)
      .directModelSubstitute(DocumentId::class.java, UUID::class.java)
      .directModelSubstitute(UserId::class.java, UUID::class.java)
      .directModelSubstitute(PrisonId::class.java, String::class.java)
      .directModelSubstitute(CourtId::class.java, String::class.java)
      .directModelSubstitute(CourtName::class.java, String::class.java)
  }
}
