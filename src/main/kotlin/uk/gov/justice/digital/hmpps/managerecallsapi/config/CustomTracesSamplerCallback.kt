package uk.gov.justice.digital.hmpps.managerecallsapi.config

import io.sentry.SamplingContext
import io.sentry.SentryOptions.TracesSamplerCallback
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class CustomTracesSamplerCallback : TracesSamplerCallback {
  override fun sample(context: SamplingContext): Double? {
    val customSamplingContext = context.customSamplingContext
    if (customSamplingContext != null) {
      val request = customSamplingContext["request"] as HttpServletRequest
      return when (request.requestURI) {
        // The health check endpoints are just noise - drop all transactions
        "/health/liveness" -> {
          0.0
        }
        "/health/readiness" -> {
          0.0
        }
        // Default sample rate
        else -> {
          0.05
        }
      }
    } else {
      return 0.05
    }
  }
}
