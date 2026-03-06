package com.ibizabroker.lms.configuration;

import com.ibizabroker.lms.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull; // <-- THÊM IMPORT NÀY
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        // ✅ LOGIC "MỀM MỎNG": Chỉ KIỂM TRA token, không bao giờ CHẶN request
        // Để Spring Security (permitAll) quyết định endpoint nào cần auth
        
        final String header = request.getHeader("Authorization");
        String username = null;
        String token = null;

        // 🔍 Bước 1: Nếu có header Authorization, thử parse token
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        
        // 🔍 SSE fallback: EventSource không gửi được header, nên chấp nhận token qua query param
        if (token == null) {
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                token = queryToken;
            }
        }
        
        if (token != null) {
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get JWT Token", e);
            } catch (ExpiredJwtException e) {
                logger.debug("JWT Token has expired", e);
            } catch (SignatureException e) {
                logger.debug("Invalid JWT signature", e);
            } catch (MalformedJwtException e) {
                logger.debug("Malformed JWT token", e);
            } catch (Exception e) {
                logger.warn("Unexpected JWT processing error", e);
            }
        } else {
            // 🔓 Không có token -> OK, cứ để qua (public endpoints không cần token)
            if (logger.isDebugEnabled()) {
                logger.debug("No JWT token found in request headers");
            }
        }

        // 🔐 Bước 2: Nếu có username hợp lệ, set Authentication vào SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails ud = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, ud)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Authentication set for user: " + username);
                    }
                }
            } catch (Exception e) {
                // ⚠️ Nếu có lỗi khi load user hoặc validate -> CHỈ LOG, KHÔNG CHẶN
                logger.warn("Could not set user authentication in security context", e);
            }
        }

        // ✅ QUAN TRỌNG: LUÔN LUÔN gọi chain.doFilter() - Cho request đi tiếp
        // Spring Security sẽ kiểm tra permitAll() và quyết định cho qua hay chặn
        chain.doFilter(request, response);
    }
}