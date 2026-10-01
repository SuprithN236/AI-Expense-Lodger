package com.aiexpenseledger.service;

import com.aiexpenseledger.domain.User;
import com.aiexpenseledger.exception.EmailAlreadyRegisteredException;
import com.aiexpenseledger.repository.UserRepository;
import com.aiexpenseledger.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    /** Compared against when the email is unknown, so login timing does not reveal which emails exist. */
    private final String timingEqualizerHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.timingEqualizerHash = passwordEncoder.encode("timing-equalizer");
    }

    public record AuthResult(String token, long expiresInSeconds, User user) {
    }

    @Transactional
    public AuthResult signup(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        User user = userRepository.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword)));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        String hash = user != null ? user.getPassword() : timingEqualizerHash;
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hash);
        if (user == null || !passwordMatches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueToken(user);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResult issueToken(User user) {
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResult(token, tokenProvider.getExpirationSeconds(), user);
    }
}
