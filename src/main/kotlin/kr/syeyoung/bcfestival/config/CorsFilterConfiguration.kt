package kr.syeyoung.bcfestival.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer


@EnableWebFlux
@Configuration
class CorsFilterConfiguration : WebFluxConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*")
    }
}

//@Configuration
////@EnableWebFlux
//class CorsFilterConfiguration {
//    @Bean
//    fun corsWebFilter(): CorsWebFilter? {
//        val corsConfig = CorsConfiguration()
//        corsConfig.allowedOrigins = listOf("**")
//        corsConfig.setMaxAge(8000L)
//        corsConfig.allowedMethods = listOf("PUT", "POST", "GET", "DELETE")
//        val source = UrlBasedCorsConfigurationSource()
//        source.registerCorsConfiguration("/**", corsConfig)
//        return CorsWebFilter(source)
//    }
//}