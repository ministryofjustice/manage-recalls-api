package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.opentelemetry.api.trace.Span
import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log: Logger = LoggerFactory.getLogger(SentryContextAppender::class.java)

@Component
class SentryContextAppender : HandlerInterceptor {
  @Throws(Exception::class)
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any?
  ): Boolean {
    val operationId: String = Span.current().spanContext.traceId

    // TODO: remove logging once we've confirmed the operationId is being captured correctly
    log.info("[preHandle] ${request.method} ${request.requestURI} - operationId: $operationId")

    Sentry.configureScope { scope ->
      scope.setContexts("appInsightsOperationId", operationId)
    }

    return true
  }
}
