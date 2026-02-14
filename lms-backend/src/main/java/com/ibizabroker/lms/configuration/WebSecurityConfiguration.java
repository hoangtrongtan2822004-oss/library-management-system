package com.ibizabroker.lms.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private final JwtRequestFilter jwtRequestFilter;
    private final AdminAuditLogFilter adminAuditLogFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Value("${allowed.origins:http://localhost:4200}")
    private String allowedOrigins;

    public WebSecurityConfiguration(JwtRequestFilter jwtRequestFilter,
                                    AdminAuditLogFilter adminAuditLogFilter,
                                    JwtAuthenticationEntryPoint authenticationEntryPoint,
                                    UserDetailsService userDetailsService) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.adminAuditLogFilter = adminAuditLogFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // RestTemplate bean is provided in HttpClientConfig with timeouts

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        
        // Set both Origins and OriginPatterns for maximum compatibility
        c.setAllowedOrigins(origins);
        c.setAllowedOriginPatterns(origins);
        
        // Allow all common HTTP methods including OPTIONS for preflight
        c.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        
        // Allow all headers including Authorization
        c.setAllowedHeaders(Arrays.asList(
            "Origin", "Access-Control-Allow-Origin", "Content-Type",
            "Accept", "Authorization", "X-Requested-With",
            "Access-Control-Request-Method", "Access-Control-Request-Headers",
            "Cookie"
        ));
        
        // Expose headers so frontend can read them
        c.setExposedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization",
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
            "Set-Cookie"
        ));
        
        // Allow credentials (cookies, authorization headers)
        c.setAllowCredentials(true);
        
        // Apply CORS config to all paths
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**", "/error", "/favicon.ico").permitAll()
                
                // 🔓 Mở khóa tạm thời để test - Cho phép xem dữ liệu công khai không cần đăng nhập
                .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/categories/**", "/api/authors/**").permitAll()
                
                // ✅ Cấu hình rõ ràng cho Chatbot để tránh 403
                // Nếu bạn muốn test, có thể đổi hasRole("USER") thành authenticated() tạm thời
                .requestMatchers("/api/user/chat/**").authenticated() 
                
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(adminAuditLogFilter, JwtRequestFilter.class);

        // 🔒 Security Headers: Chỉ cho phép H2 Console trong iframe (cùng origin), chặn Clickjacking
        http.headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin()) // Thay vì disable() - chỉ cho phép iframe từ cùng domain
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; base-uri 'self'; frame-ancestors 'self';")
            )
        );
        return http.build();
    }
}