package com.techinsights.api.aid

/**
 * The aid cookie is unsigned, so its value is client-controlled. The server only ever issues UUID v4
 * ([UuidV4Generator]); anything else is rejected so a client cannot choose its own identity
 * (e.g. `U_<userId>`, which is how authenticated views are keyed) for view de-duplication or read history.
 */
object AidFormat {
  private val ISSUED = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")

  fun isValid(value: String): Boolean = ISSUED.matches(value)
}
