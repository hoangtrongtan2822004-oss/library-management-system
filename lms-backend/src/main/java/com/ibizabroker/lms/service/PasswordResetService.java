package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.PasswordResetTokenRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.PasswordResetToken;
import com.ibizabroker.lms.entity.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UsersRepository usersRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final Duration tokenValidity;
    private final String resetBaseUrl;

    public PasswordResetService(UsersRepository usersRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                @Value("${lms.reset-password.token-validity-minutes:30}") long tokenValidityMinutes,
                                @Value("${lms.reset-password.url:http://localhost:4200/reset-password}") String resetBaseUrl) {
        this.usersRepository = usersRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenValidity = Duration.ofMinutes(tokenValidityMinutes);
        this.resetBaseUrl = resetBaseUrl;
    }

    @Transactional
    public void initiateReset(String email) {
        String normalizedEmail = Optional.ofNullable(email)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");

        Optional<Users> userOptional = usersRepository.findFirstByEmailIgnoreCaseOrderByUserIdAsc(normalizedEmail);

        // Luôn trả về thành công để tránh lộ thông tin email tồn tại hay không
        if (userOptional.isEmpty()) {
            return;
        }

        Users user = userOptional.get();
        tokenRepository.deleteAllByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plus(tokenValidity);
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        tokenRepository.save(resetToken);

        String link = buildResetLink(token);
        String subject = "Đặt lại mật khẩu thư viện";
        String htmlContent = "<p>Chào " + (user.getName() != null ? user.getName() : "bạn") + ",</p>"
                + "<p>Bạn vừa yêu cầu đặt lại mật khẩu tài khoản thư viện. Nhấn vào liên kết dưới đây để đặt lại mật khẩu. "
                + "Liên kết sẽ hết hạn sau " + tokenValidity.toMinutes() + " phút.</p>"
                + "<p><a href='" + link + "'>Đặt lại mật khẩu</a></p>"
                + "<p>Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p>";

        emailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không hợp lệ"));

        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã được sử dụng");
        }
        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn");
        }

        Users user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private String buildResetLink(String token) {
        if (resetBaseUrl.contains("?")) {
            return resetBaseUrl + "&token=" + token;
        }
        return resetBaseUrl + "?token=" + token;
    }
}
