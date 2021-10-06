package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reference-data", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class ReferenceDataController {

  @GetMapping("/local-delivery-units")
  fun localDeliveryUnits(): List<LocalDeliveryUnitResponse> = LocalDeliveryUnit.values().map { ldu -> LocalDeliveryUnitResponse(ldu.name, ldu.label) }.toList()
}

data class LocalDeliveryUnitResponse(val name: String, val label: String)
