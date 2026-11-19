package com.eventsphere.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Matches every request under /api/ so the REST API gets JSON-friendly security behaviour. */
    private static final RequestMatcher API_REQUESTS = (HttpServletRequest request) ->
            request.getRequestURI().startsWith(request.getContextPath() + "/api/");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // public pages and assets
                .requestMatchers("/", "/events", "/events/{id}", "/login", "/register",
                        "/css/**", "/images/**", "/error", "/actuator/health", "/h2-console/**").permitAll()
                // public read-only API
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                // admin-only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
                // everything else needs a logged-in user
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/events", false)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/?logout")
                .permitAll())
            // REST clients (Postman, curl) authenticate with HTTP Basic
            .httpBasic(Customizer.withDefaults())
            // API callers get a plain 401 instead of being redirected to the login page
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_REQUESTS))
            // the stateless API and the H2 console don't use CSRF tokens; HTML forms still do
            .csrf(csrf -> csrf.ignoringRequestMatchers(API_REQUESTS)
                .ignoringRequestMatchers("/h2-console/**"))
            // the H2 console runs inside a frame
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
