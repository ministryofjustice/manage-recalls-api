package uk.gov.justice.digital.hmpps.managerecallsapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.SpringDocUtils
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ColumnName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import java.util.UUID

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  companion object {
    init {
      SpringDocUtils.getConfig()
        .replaceWithClass(BookingNumber::class.java, String::class.java)
        .replaceWithClass(ColumnName::class.java, String::class.java)
        .replaceWithClass(CourtId::class.java, String::class.java)
        .replaceWithClass(CourtName::class.java, String::class.java)
        .replaceWithClass(CroNumber::class.java, String::class.java)
        .replaceWithClass(DocumentId::class.java, UUID::class.java)
        .replaceWithClass(Email::class.java, String::class.java)
        .replaceWithClass(FieldName::class.java, String::class.java)
        .replaceWithClass(FieldPath::class.java, String::class.java)
        .replaceWithClass(FileName::class.java, String::class.java)
        .replaceWithClass(FirstName::class.java, String::class.java)
        .replaceWithClass(FullName::class.java, String::class.java)
        .replaceWithClass(LastKnownAddressId::class.java, UUID::class.java)
        .replaceWithClass(LastName::class.java, String::class.java)
        .replaceWithClass(MiddleNames::class.java, String::class.java)
        .replaceWithClass(MissingDocumentsRecordId::class.java, UUID::class.java)
        .replaceWithClass(NomsNumber::class.java, String::class.java)
        .replaceWithClass(NoteId::class.java, UUID::class.java)
        .replaceWithClass(PartBRecordId::class.java, UUID::class.java)
        .replaceWithClass(PhoneNumber::class.java, String::class.java)
        .replaceWithClass(PoliceForceId::class.java, String::class.java)
        .replaceWithClass(PoliceForceName::class.java, String::class.java)
        .replaceWithClass(PrisonId::class.java, String::class.java)
        .replaceWithClass(PrisonName::class.java, String::class.java)
        .replaceWithClass(RecallId::class.java, UUID::class.java)
        .replaceWithClass(RescindRecordId::class.java, UUID::class.java)
        .replaceWithClass(UserId::class.java, UUID::class.java)
        .replaceWithClass(WarrantReferenceNumber::class.java, String::class.java)
    }
  }

  @Bean
  fun customOpenAPI(): OpenAPI? = OpenAPI()
    .info(
      Info().title("Manage Recalls API").version(version).description(
        "Back end for Manage Recalls UI.  Future API for Recalls: not currently the operational system of reference, which remains PPUD."
      )
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk"))
    )
}
