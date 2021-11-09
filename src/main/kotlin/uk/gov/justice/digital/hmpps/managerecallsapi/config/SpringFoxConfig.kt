package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.classmate.ResolvedType
import com.fasterxml.classmate.TypeResolver
import com.fasterxml.classmate.types.ResolvedArrayType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
import org.springframework.web.method.HandlerMethod
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver
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

  @Bean
  @Primary
  fun fluxMethodResolver(resolver: TypeResolver?): HandlerMethodResolver? {
    return object : HandlerMethodResolver(resolver) {
      override fun methodReturnType(handlerMethod: HandlerMethod): ResolvedType {
        var retType = super.methodReturnType(handlerMethod)
        // we unwrap Mono, Flux, and as a bonus - ResponseEntity
        while (retType.erasedType == Mono::class.java || retType.erasedType == Flux::class.java || retType.erasedType == ResponseEntity::class.java) {
          retType = if (retType.erasedType == Flux::class.java) {
            // treat it as an array
            val type = retType.typeBindings.getBoundType(0)
            ResolvedArrayType(type.erasedType, type.typeBindings, type)
          } else {
            retType.typeBindings.getBoundType(0)
          }
        }
        return retType
      }
    }
  }
}
