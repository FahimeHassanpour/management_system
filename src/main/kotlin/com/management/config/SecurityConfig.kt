package com.management.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/login").permitAll()
                it.requestMatchers("/register").permitAll()
                it.requestMatchers("/admin/**").hasRole("ADMIN")
                it.requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")
                it.anyRequest().authenticated()
            }
            .formLogin {
                it.loginPage("/login")
                it.usernameParameter("username")
                it.passwordParameter("password")
                it.failureUrl("/login?error")
                it.defaultSuccessUrl("/dashboard", true)
                it.permitAll()
            }
            .logout {
                it.logoutSuccessUrl("/login?logout")
            }
            .httpBasic { it.disable() }

        return http.build()
    }

    @Bean
    fun authenticationManager(
        authenticationConfiguration: AuthenticationConfiguration
    ): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
}