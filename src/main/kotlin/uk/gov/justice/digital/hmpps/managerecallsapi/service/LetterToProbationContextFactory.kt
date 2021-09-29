package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId

@Component
class LetterToProbationContextFactory {
  fun createContext(recallId: RecallId, userId: UserId): Mono<LetterToProbationContext> {
    return Mono.just(LetterToProbationContext())
  }
}

class LetterToProbationContext
