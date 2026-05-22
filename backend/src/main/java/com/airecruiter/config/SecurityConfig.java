package com.airecruiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * CURRENT (Phase 1 / local dev):
 *   All endpoints are open — no auth required.
 *   This is intentional for local development only.
 *
 * PHASE 3 TODO — Replace with JWT auth:
 *   1. Add spring-boot-starter-oauth2-resource-server to pom.xml
 *   2. Uncomment the .authorizeHttpRequests() block below
 *   3. Configure your JWT issuer-uri in application.yml
 *   4. Protect all /api/** endpoints with hasRole("RECRUITER")
 *
 * NEVER deploy the current open config to a public environment.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless REST API, not session-based
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no HTTP sessions
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Phase 1: permit everything (local dev only)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

            /*
             * Phase 3 — uncomment to enable JWT protection:
             *
             * .authorizeHttpRequests(auth -> auth
             *     .requestMatchers("/actuator/health").permitAll()
             *     .requestMatchers("/api/**").hasRole("RECRUITER")
             *     .anyRequest().authenticated())
             * .oauth2ResourceServer(oauth2 ->
             *     oauth2.jwt(Customizer.withDefaults()));
             */

        return http.build();
    }
}
