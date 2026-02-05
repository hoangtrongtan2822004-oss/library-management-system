package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.OAuth2LoginResponse;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.service.OAuth2UserService;
import com.ibizabroker.lms.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2 Authentication Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final OAuth2UserService oAuth2UserService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:4200/auth/google/callback}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id:}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret:}")
    private String facebookClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.redirect-uri:http://localhost:4200/auth/facebook/callback}")
    private String facebookRedirectUri;

    /**
     * Google OAuth2 callback endpoint
     * Exchanges authorization code for tokens and creates/updates user
     */
    @PostMapping("/google/callback")
    public ResponseEntity<OAuth2LoginResponse> googleCallback(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Exchange authorization code for access token
            String tokenEndpoint = "https://oauth2.googleapis.com/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format(
                "code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code",
                code, googleClientId, googleClientSecret, redirectUri
            );

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>(){}
            );

            Map<String, Object> tokenData = Objects.requireNonNullElse(tokenResponse.getBody(), Collections.emptyMap());
            String accessToken = (String) tokenData.get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("No access token returned from token endpoint: {}", tokenData);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Get user info from Google
            String userInfoEndpoint = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            
            HttpEntity<?> userInfoEntity = new HttpEntity<>(userInfoHeaders);
            ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                userInfoEndpoint,
                HttpMethod.GET,
                userInfoEntity,
                new ParameterizedTypeReference<Map<String, Object>>(){}
            );

            Map<String, Object> userAttributes = Objects.requireNonNullElse(userInfoResponse.getBody(), Collections.emptyMap());

            // Check if this is a new user
            String googleId = (String) userAttributes.get("sub");
            boolean isNewUser = (googleId == null || googleId.isEmpty()) 
                    || oAuth2UserService.findByGoogleId(googleId).isEmpty();

            // Process OAuth2 user (create or update)
            Users user = oAuth2UserService.processOAuth2User(userAttributes);

            // Get user's primary role
            String role = user.getRoles().stream()
                    .findFirst()
                    .map(r -> r.getRoleName())
                    .orElse("USER");

            // Create UserDetails for JWT generation
            UserDetails userDetails = User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                    .build();

            // Generate JWT token with extra claims
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", user.getUserId());
            extraClaims.put("role", role);
            String jwtToken = jwtUtil.generateToken(userDetails, extraClaims);

            OAuth2LoginResponse response = new OAuth2LoginResponse(
                jwtToken,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                role,
                isNewUser
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing Google OAuth2 callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get Google OAuth2 configuration for frontend
     */
    @GetMapping("/google/config")
    public ResponseEntity<Map<String, String>> getGoogleConfig() {
        return ResponseEntity.ok(Map.of(
            "clientId", googleClientId,
            "redirectUri", redirectUri
        ));
    }

    /**
     * Facebook OAuth2 callback endpoint
     * Exchanges authorization code for tokens and creates/updates user
     */
    @PostMapping("/facebook/callback")
    public ResponseEntity<OAuth2LoginResponse> facebookCallback(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Exchange authorization code for access token
            String tokenEndpoint = "https://graph.facebook.com/v18.0/oauth/access_token";
            
            String tokenUrl = String.format(
                "%s?client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                tokenEndpoint, facebookClientId, facebookClientSecret, facebookRedirectUri, code
            );

            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                tokenUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>(){}
            );

            Map<String, Object> tokenData = Objects.requireNonNullElse(tokenResponse.getBody(), Collections.emptyMap());
            String accessToken = (String) tokenData.get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("No access token returned from Facebook token endpoint: {}", tokenData);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Get user info from Facebook
            String userInfoEndpoint = "https://graph.facebook.com/me?fields=id,name,email,picture";
            String userInfoUrl = userInfoEndpoint + "&access_token=" + accessToken;
            
            ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>(){}
            );

            Map<String, Object> userAttributes = Objects.requireNonNullElse(userInfoResponse.getBody(), Collections.emptyMap());

            // Check if this is a new user
            String facebookId = (String) userAttributes.get("id");
            boolean isNewUser = (facebookId == null || facebookId.isEmpty()) 
                    || oAuth2UserService.findByFacebookId(facebookId).isEmpty();

            // Process Facebook user (create or update)
            Users user = oAuth2UserService.processFacebookUser(userAttributes);

            // Get user's primary role
            String role = user.getRoles().stream()
                    .findFirst()
                    .map(r -> r.getRoleName())
                    .orElse("USER");

            // Create UserDetails for JWT generation
            UserDetails userDetails = User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                    .build();

            // Generate JWT token with extra claims
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", user.getUserId());
            extraClaims.put("role", role);
            String jwtToken = jwtUtil.generateToken(userDetails, extraClaims);

            OAuth2LoginResponse response = new OAuth2LoginResponse(
                jwtToken,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                role,
                isNewUser
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing Facebook OAuth2 callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get Facebook OAuth2 configuration for frontend
     */
    @GetMapping("/facebook/config")
    public ResponseEntity<Map<String, String>> getFacebookConfig() {
        return ResponseEntity.ok(Map.of(
            "clientId", facebookClientId,
            "redirectUri", facebookRedirectUri
        ));
    }

    /**
     * Generic social token endpoint for popup flows
     * Accepts { provider: 'facebook'|'google', accessToken: '<provider access token>' }
     */
    @PostMapping("/social")
    public ResponseEntity<OAuth2LoginResponse> socialLogin(@RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String accessToken = request.get("accessToken");

        if (provider == null || provider.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Map<String, Object> userAttributes = Collections.emptyMap();

            if ("facebook".equalsIgnoreCase(provider)) {
                // Get user info from Facebook
                String userInfoEndpoint = "https://graph.facebook.com/me?fields=id,name,email,picture";
                String userInfoUrl = userInfoEndpoint + "&access_token=" + accessToken;

                ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>(){}
                );

                userAttributes = Objects.requireNonNullElse(userInfoResponse.getBody(), Collections.emptyMap());

                // Process Facebook user
                Users user = oAuth2UserService.processFacebookUser(userAttributes);

                // Build JWT and response
                String role = user.getRoles().stream()
                        .findFirst()
                        .map(r -> r.getRoleName())
                        .orElse("USER");

                UserDetails userDetails = User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                        .build();

                Map<String, Object> extraClaims = new HashMap<>();
                extraClaims.put("userId", user.getUserId());
                extraClaims.put("role", role);
                String jwtToken = jwtUtil.generateToken(userDetails, extraClaims);

                boolean isNewUser = (userAttributes.get("id") == null) || oAuth2UserService.findByFacebookId((String)userAttributes.get("id")).isEmpty();

                OAuth2LoginResponse response = new OAuth2LoginResponse(
                    jwtToken,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    role,
                    isNewUser
                );

                return ResponseEntity.ok(response);

            } else if ("google".equalsIgnoreCase(provider)) {
                // Get user info from Google using Bearer token
                String userInfoEndpoint = "https://www.googleapis.com/oauth2/v2/userinfo";

                HttpHeaders userInfoHeaders = new HttpHeaders();
                userInfoHeaders.setBearerAuth(accessToken);
                HttpEntity<?> userInfoEntity = new HttpEntity<>(userInfoHeaders);

                ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                    userInfoEndpoint,
                    HttpMethod.GET,
                    userInfoEntity,
                    new ParameterizedTypeReference<Map<String, Object>>(){}
                );

                userAttributes = Objects.requireNonNullElse(userInfoResponse.getBody(), Collections.emptyMap());

                // Process Google user
                Users user = oAuth2UserService.processOAuth2User(userAttributes);

                String role = user.getRoles().stream()
                        .findFirst()
                        .map(r -> r.getRoleName())
                        .orElse("USER");

                UserDetails userDetails = User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                        .build();

                Map<String, Object> extraClaims = new HashMap<>();
                extraClaims.put("userId", user.getUserId());
                extraClaims.put("role", role);
                String jwtToken = jwtUtil.generateToken(userDetails, extraClaims);

                boolean isNewUser = (userAttributes.get("sub") == null) || oAuth2UserService.findByGoogleId((String)userAttributes.get("sub")).isEmpty();

                OAuth2LoginResponse response = new OAuth2LoginResponse(
                    jwtToken,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    role,
                    isNewUser
                );

                return ResponseEntity.ok(response);

            } else {
                return ResponseEntity.badRequest().build();
            }

        } catch (Exception e) {
            log.error("Error processing social login for provider {}", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
