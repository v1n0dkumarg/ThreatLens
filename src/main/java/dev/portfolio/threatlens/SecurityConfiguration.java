package dev.portfolio.threatlens;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder,
            @Value("${security.users.admin.username}") String adminName,
            @Value("${security.users.admin.password}") String adminPassword,
            @Value("${security.users.analyst.username}") String analystName,
            @Value("${security.users.analyst.password}") String analystPassword,
            @Value("${security.users.viewer.username}") String viewerName,
            @Value("${security.users.viewer.password}") String viewerPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminName).password(encoder.encode(adminPassword)).roles("ADMIN", "ANALYST", "VIEWER").build(),
                User.withUsername(analystName).password(encoder.encode(analystPassword)).roles("ANALYST", "VIEWER").build(),
                User.withUsername(viewerName).password(encoder.encode(viewerPassword)).roles("VIEWER").build());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/auth.css", "/error", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole("ANALYST", "ADMIN")
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.realmName("ThreatLens")
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}