package org.shopwave.smartclinic.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security configuration.
 * <p>
 * - Form login with a single login page; role determines post-login redirect.
 * - Role-based URL authorization (ADMIN/DOCTOR/PATIENT).
 * - Session-based logout.
 * - CSRF disabled for REST API endpoints; enabled for Thymeleaf form login/logout.
 *
 * @see ClinicUserDetailsService
 * @see ClinicUserDetails
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClinicUserDetailsService userDetailsService;

    public SecurityConfig(ClinicUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(new LoginRateLimitingFilter(),
                    UsernamePasswordAuthenticationFilter.class)

            // Authorize requests
            .authorizeHttpRequests(auth -> auth
                // Public: login, error, static resources
                .requestMatchers(
                    "/login", "/signup", "/forgot-password", "/reset-password", "/login-error",
                    "/css/**", "/js/**", "/images/**", "/clinic.css", "/clinic.js",
                    "/api/public/**", "/api/auth/forgot-password", "/api/auth/reset-password"
                ).permitAll()

                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Doctor only
                .requestMatchers("/doctor/**").hasRole("DOCTOR")

                // Patient only
                .requestMatchers("/patient/**").hasRole("PATIENT")

                // API endpoints: role-based
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                .requestMatchers("/api/patient/**").hasRole("PATIENT")
                .requestMatchers("/api/public/**").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Form login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform-login")
                .defaultSuccessUrl("/role-redirect", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/perform-logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // CSRF: disable for stateless API, enable for form login
            .csrf(csrf -> csrf
               .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
               .csrfTokenRequestHandler(csrfRequestHandler)
               .ignoringRequestMatchers("/perform-login", "/perform-logout")
             );

        return http.build();
    }
}
