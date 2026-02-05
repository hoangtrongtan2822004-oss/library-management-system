package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.RoleRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Role;
import com.ibizabroker.lms.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * OAuth2 User Service - Handle Google and Facebook OAuth2 authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Process OAuth2 user login from Google
     * Creates new user if not exists, or returns existing user
     */
    @Transactional
    public Users processOAuth2User(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");
        String picture = (String) attributes.get("picture");

        log.info("Processing Google OAuth2 login for email: {}", email);

        // Check if user already exists by email
        Optional<Users> existingUser = usersRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            Users user = existingUser.get();
            // Update Google ID and profile if not set
            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                user.setGoogleId(googleId);
                user.setProfilePicture(picture);
                usersRepository.save(user);
            }
            log.info("Existing user logged in via Google: {}", email);
            return user;
        }

        // Create new user from Google profile
        return createOAuth2User(email, name, googleId, picture, "google");
    }

    /**
     * Process OAuth2 user login from Facebook
     * Creates new user if not exists, or returns existing user
     */
    @Transactional
    public Users processFacebookUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String facebookId = (String) attributes.get("id");
        
        // Facebook picture URL format (safe runtime checks to avoid unchecked casts)
        String picture = null;
        Object picObj = attributes.get("picture");
        if (picObj instanceof Map<?, ?>) {
            Map<?, ?> pictureData = (Map<?, ?>) picObj;
            Object dataObj = pictureData.get("data");
            if (dataObj instanceof Map<?, ?>) {
                Map<?, ?> data = (Map<?, ?>) dataObj;
                Object urlObj = data.get("url");
                if (urlObj instanceof String) {
                    picture = (String) urlObj;
                }
            }
        }

        log.info("Processing Facebook OAuth2 login for email: {}", email);

        // Check if user already exists by email
        Optional<Users> existingUser = usersRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            Users user = existingUser.get();
            // Update Facebook ID and profile if not set
            if (user.getFacebookId() == null || !user.getFacebookId().equals(facebookId)) {
                user.setFacebookId(facebookId);
                user.setProfilePicture(picture);
                usersRepository.save(user);
            }
            log.info("Existing user logged in via Facebook: {}", email);
            return user;
        }

        // Create new user from Facebook profile
        return createOAuth2User(email, name, facebookId, picture, "facebook");
    }

    /**
     * Create new OAuth2 user (shared logic for Google and Facebook)
     */
    private Users createOAuth2User(String email, String name, String providerId, String picture, String provider) {
        Users newUser = new Users();
        newUser.setUsername(generateUsernameFromEmail(email));
        newUser.setEmail(email);
        newUser.setFullName(name);
        
        if ("google".equals(provider)) {
            newUser.setGoogleId(providerId);
        } else if ("facebook".equals(provider)) {
            newUser.setFacebookId(providerId);
        }
        
        newUser.setProfilePicture(picture);
        
        // Set random password (won't be used for OAuth2 login)
        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        
        // Assign USER role by default
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found in database"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        Users savedUser = usersRepository.save(newUser);
        log.info("New user created via {} OAuth2: {}", provider, email);
        
        return savedUser;
    }

    /**
     * Generate unique username from email
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = baseUsername;
        int counter = 1;

        // Ensure username is unique
        while (usersRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    /**
     * Check if user exists by Google ID
     */
    @Transactional(readOnly = true)
    public Optional<Users> findByGoogleId(String googleId) {
        return usersRepository.findByGoogleId(googleId);
    }
    
    /**
     * Check if user exists by Facebook ID
     */
    @Transactional(readOnly = true)
    public Optional<Users> findByFacebookId(String facebookId) {
        return usersRepository.findByFacebookId(facebookId);
    }
}
