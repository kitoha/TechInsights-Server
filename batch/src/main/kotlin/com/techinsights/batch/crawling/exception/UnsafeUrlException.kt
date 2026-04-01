package com.techinsights.batch.crawling.exception

class UnsafeUrlException(url: String) : RuntimeException("Blocked unsafe URL: $url")
