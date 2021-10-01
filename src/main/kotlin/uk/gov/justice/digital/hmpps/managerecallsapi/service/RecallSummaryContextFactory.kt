package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class RecallSummaryContextFactory {

  fun createRecallSummaryContext(recallNotificationContext: RecallNotificationContext): Mono<RecallSummaryContext> {
    return Mono.just(
      RecallSummaryContext(
        recallNotificationContext.recall,
        recallNotificationContext.prisoner,
        recallNotificationContext.lastReleasePrisonName,
        recallNotificationContext.currentPrisonName,
        recallNotificationContext.assessedByUserDetails
      )
    )
  }
}
