package com.management.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class LoginAuthenticationFailureHandler : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val redirectUrl =
            if (exception is DisabledException || hasDisabledCause(exception)) {
                "/login?disabled"
            } else {
                "/login?error"
            }
        redirectStrategy.sendRedirect(request, response, redirectUrl)
    }

    private fun hasDisabledCause(exception: AuthenticationException): Boolean {
        var cause: Throwable? = exception.cause
        while (cause != null) {
            if (cause is DisabledException) return true
            cause = cause.cause
        }
        return false
    }
}
