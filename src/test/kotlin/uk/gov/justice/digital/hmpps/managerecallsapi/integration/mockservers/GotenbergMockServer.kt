package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import org.springframework.stereotype.Component

@Component
class GotenbergMockServer : WireMockServer(9998)
