package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.classmate.ResolvedType
import com.fasterxml.classmate.TypeResolver
import com.fasterxml.classmate.types.ResolvedArrayType
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType
import org.springframework.boot.actuate.endpoint.ExposableEndpoint
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver
import org.springframework.boot.actuate.endpoint.web.EndpointMapping
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils.hasText
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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
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
      .directModelSubstitute(MiddleNames::class.java, String::class.java)
      .directModelSubstitute(LastName::class.java, String::class.java)
      .directModelSubstitute(FullName::class.java, String::class.java)
      .directModelSubstitute(Email::class.java, String::class.java)
      .directModelSubstitute(PhoneNumber::class.java, String::class.java)
      .directModelSubstitute(RecallId::class.java, UUID::class.java)
      .directModelSubstitute(DocumentId::class.java, UUID::class.java)
      .directModelSubstitute(MissingDocumentsRecordId::class.java, UUID::class.java)
      .directModelSubstitute(UserId::class.java, UUID::class.java)
      .directModelSubstitute(PrisonId::class.java, String::class.java)
      .directModelSubstitute(PrisonName::class.java, String::class.java)
      .directModelSubstitute(CourtId::class.java, String::class.java)
      .directModelSubstitute(CourtName::class.java, String::class.java)
      .directModelSubstitute(PoliceForceId::class.java, String::class.java)
      .directModelSubstitute(PoliceForceName::class.java, String::class.java)
      .directModelSubstitute(FieldName::class.java, String::class.java)
      .directModelSubstitute(FieldPath::class.java, String::class.java)
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

  // Fixes incompatibility with latest spring-boot: https://github.com/springfox/springfox/issues/3462
  @Bean
  fun webEndpointServletHandlerMapping(
    webEndpointsSupplier: WebEndpointsSupplier,
    servletEndpointsSupplier: ServletEndpointsSupplier,
    controllerEndpointsSupplier: ControllerEndpointsSupplier,
    endpointMediaTypes: EndpointMediaTypes?,
    corsProperties: CorsEndpointProperties,
    webEndpointProperties: WebEndpointProperties,
    environment: Environment
  ): WebMvcEndpointHandlerMapping? {
    val allEndpoints: MutableList<ExposableEndpoint<*>?> = ArrayList()
    val webEndpoints = webEndpointsSupplier.endpoints
    allEndpoints.addAll(webEndpoints)
    allEndpoints.addAll(servletEndpointsSupplier.endpoints)
    allEndpoints.addAll(controllerEndpointsSupplier.endpoints)
    val basePath = webEndpointProperties.basePath
    val endpointMapping = EndpointMapping(basePath)
    val shouldRegisterLinksMapping = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath)
    return WebMvcEndpointHandlerMapping(
      endpointMapping,
      webEndpoints,
      endpointMediaTypes,
      corsProperties.toCorsConfiguration(),
      EndpointLinksResolver(allEndpoints, basePath),
      shouldRegisterLinksMapping,
      null
    )
  }

  private fun shouldRegisterLinksMapping(
    webEndpointProperties: WebEndpointProperties,
    environment: Environment,
    basePath: String
  ): Boolean {
    return webEndpointProperties.discovery.isEnabled && (
      hasText(basePath) || ManagementPortType.get(
        environment
      ) == ManagementPortType.DIFFERENT
      )
  }
  // --- END ---
}
