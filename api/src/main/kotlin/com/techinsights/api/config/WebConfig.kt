package com.techinsights.api.config

import com.techinsights.api.aid.AidInterceptor
import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.RequesterResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val corsProperties: CorsProperties,
    private val interceptor: AidInterceptor,
    private val props: AidProperties
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns(props.applyPaths)
            .excludePathPatterns(props.excludePaths)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(RequesterResolver(props))
    }
}
