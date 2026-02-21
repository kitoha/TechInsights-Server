package com.techinsights.domain.utils

import io.hypersistence.tsid.TSID

fun String.decode(): Long = Tsid.decode(this)
fun Long.encode(): String = Tsid.encode(this)

object Tsid {
  fun generate(): String {
    return TSID.Factory.getTsid().toString();
  }

  fun generateLong(): Long {
    return TSID.Factory.getTsid().toLong()
  }

  fun encode(id: Long): String {
    return TSID.from(id).toString()
  }

  fun decode(id: String): Long {
    return TSID.from(id).toLong()
  }
}