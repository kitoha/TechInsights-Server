package com.techinsights.api.aid

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("aid")
data class AidProperties(
  var applyPaths: List<String> = listOf("/api/**"),
  var excludePaths: List<String> = listOf("/assets/**","/static/**","/actuator/**","/health"),
  var cookie: Cookie = Cookie()
) {
  data class Cookie(
    var name: String = "aid",
    var domain: String? = null,
    var ttlDays: Long = 365,
    var sameSiteDefault: String = "Lax",
    var secureMode: SecureMode = SecureMode.AUTO
  )
  enum class SecureMode { ALWAYS, NEVER, AUTO }
}
