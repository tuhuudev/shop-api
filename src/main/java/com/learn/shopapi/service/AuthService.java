package com.learn.shopapi.service;

import com.learn.shopapi.dto.AuthResponse;
import com.learn.shopapi.dto.LoginRequest;
import com.learn.shopapi.dto.RegisterRequest;
import com.learn.shopapi.dto.UserResponse;
import com.learn.shopapi.entity.Customer;
import com.learn.shopapi.entity.RefreshToken;
import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.entity.LoginOutcome;
import com.learn.shopapi.exception.RefreshTokenReuseException;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.exception.TooManyAttemptsException;
import com.learn.shopapi.repository.CustomerRepository;
import com.learn.shopapi.repository.RoleRepository;
import com.learn.shopapi.repository.UserRepository;
import com.learn.shopapi.security.JwtService;
import com.learn.shopapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Logic nghiep vu cho xac thuc: dang ky, dang nhap, refresh, logout, lay user hien tai.
 * Day la noi rap noi PasswordEncoder + AuthenticationManager + JwtService + RefreshTokenService.
 */
@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final LoginEventService loginEventService;
    private final long accessTokenExpirationMs;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                       JwtService jwtService, RefreshTokenService refreshTokenService,
                       LoginAttemptService loginAttemptService, LoginEventService loginEventService,
                       @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.loginEventService = loginEventService;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Thong bao CHUNG CHUNG (khong noi ro trung username hay email) de tranh lo
        // tai khoan nao da ton tai (user enumeration).
        if (userRepository.existsByUsername(req.username()) || userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Tai khoan hoac email da duoc su dung");
        }

        Role customerRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Thieu role mac dinh: " + DEFAULT_ROLE));

        // Quan trong: luu MAT KHAU DA BAM, khong luu mat khau goc.
        User user = new User(req.username(), passwordEncoder.encode(req.password()),
                req.email(), req.fullName());
        user.addRole(customerRole);
        userRepository.save(user);

        // Tao ho so Customer gan voi tai khoan -> sau nay dat don lay tu day.
        String displayName = (req.fullName() == null || req.fullName().isBlank())
                ? req.username() : req.fullName();
        customerRepository.save(new Customer(displayName, req.email(), user));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String username = req.username();
        // Chong do mat khau: tu choi som neu tai khoan dang bi khoa tam thoi.
        if (loginAttemptService.isBlocked(username)) {
            loginEventService.record(username, LoginOutcome.BLOCKED);
            throw new TooManyAttemptsException(
                    "Dang nhap sai qua nhieu lan. Vui long thu lai sau it phut.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.password()));
            loginAttemptService.recordSuccess(username);   // dang nhap dung -> xoa lich su sai
            loginEventService.record(username, LoginOutcome.SUCCESS);

            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
            return buildAuthResponse(user);
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(username);   // sai -> tang dem
            loginEventService.record(username, LoginOutcome.FAILURE);
            throw ex;                                       // GlobalExceptionHandler -> 401
        }
    }

    // @Transactional: giu session mo de doc lazy (rt.getUser()) va de rotate atomic.
    // Khi phat hien tai su dung, viec "thu hoi tat ca" van COMMIT doc lap nho REQUIRES_NEW
    // (xem RefreshTokenService.revokeAllForUser), du transaction nay bi rollback boi loi 401.
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken rt;
        try {
            rt = refreshTokenService.verifyUsable(refreshToken);
        } catch (RefreshTokenReuseException ex) {
            // Token da thu hoi lai duoc dung -> nghi bi danh cap -> thu hoi HET (tx rieng) roi bao 401.
            refreshTokenService.revokeAllForUser(ex.getUser());
            throw new BadCredentialsException("Refresh token da bi thu hoi (nghi bi lo, da khoa tat ca)");
        }
        User user = rt.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        // XOAY VONG: thu hoi refresh token cu, cap token moi.
        String newRefreshToken = refreshTokenService.rotate(rt);
        return AuthResponse.of(newAccessToken, newRefreshToken, accessTokenExpirationMs,
                user.getUsername(), roleNames(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResourceNotFoundException("Chua dang nhap"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
        // Phai biet mat khau cu moi cho doi (chong nguoi muon token tam doi mat khau).
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("Mat khau cu khong dung");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        // Doi mat khau -> thu hoi het refresh token cu (buoc dang nhap lai o noi khac).
        refreshTokenService.revokeAllForUser(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResourceNotFoundException("Chua dang nhap"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
        return UserResponse.from(user);
    }

    // ---- helper dung chung ----

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.create(user);
        return AuthResponse.of(accessToken, refreshToken, accessTokenExpirationMs,
                user.getUsername(), roleNames(user));
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }
}
