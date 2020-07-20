package no.nav.arbeidsplassen.analytics

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiConfig : WebMvcConfigurer {

    private var origins = arrayOf("http://localhost:3000")


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*origins)
    }
}