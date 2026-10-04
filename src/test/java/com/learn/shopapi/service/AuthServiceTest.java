package com.learn.shopapi.service;

import com.learn.shopapi.dto.AuthResponse;
import com.learn.shopapi.dto.LoginRequest;
import com.learn.shopapi.dto.RegisterRequest;
import com.learn.shopapi.entity.Customer;
import com.learn.shopapi.entity.LoginOutcome;
import com.learn.shopapi.entity.RefreshToken;
import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.RefreshTokenReuseException;
import com.learn.shopapi.exception.TooManyAttemptsException;
import com.learn.shopapi.repository.CustomerRepository;
import com.learn.shopapi.repository.RoleRepository;
import com.learn.shopapi.repository.UserRepository;
import com.learn.shopapi.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test AuthService (Mockito): dang ky, dang nhap + khoa do mat khau, refresh/rotation, doi mat khau. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long TTL = 900_000L;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private LoginEventService loginEventService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, customerRepository, passwordEncoder,
                authenticationManager, userDetailsService, jwtService, refreshTokenService,
                loginAttemptService, loginEventService, TTL);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User customer(String username) {
        User u = new User(username, "{hash}", username + "@mail.vn", null);
        u.addRole(new Role("CUSTOMER"));
        return u;
    }

    private void stubTokens(String username) {
        UserDetails details = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(details);
        when(jwtService.generateAccessToken(details)).thenReturn("access");
        when(refreshTokenService.create(any(User.class))).thenReturn("refresh");
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, null, List.of()));
    }

    // ---- register ----

    @Test
    void register_bamMatKhau_ganRoleCustomer_taoCustomer() {
        var req = new RegisterRequest("alice", "secret123", "alice@mail.vn", null);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@mail.vn")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(new Role("CUSTOMER")));
        when(passwordEncoder.encode("secret123")).thenReturn("{bcrypt}xyz");
        stubTokens("alice");

        AuthResponse res = authService.register(req);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("{bcrypt}xyz");
        ArgumentCaptor<Customer> cust = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(cust.capture());
        assertThat(res.username()).isEqualTo("alice");
        assertThat(res.roles()).containsExactly("CUSTOMER");
        assertThat(res.accessToken()).isEqualTo("access");
        assertThat(res.refreshToken()).isEqualTo("refresh");
        assertThat(res.expiresInMs()).isEqualTo(TTL);
    }

    @Test
    void register_trungEmail_thongBaoChungChung_khongLuu() {
        var req = new RegisterRequest("bob", "secret123", "taken@mail.vn", "Bob");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("taken@mail.vn")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tai khoan hoac email da duoc su dung");
        verify(userRepository, never()).save(any());
    }

    // ---- login ----

    @Test
    void login_dung_xoaDemSai_ghiSuccess() {
        User alice = customer("alice");
        when(loginAttemptService.isBlocked("alice")).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        stubTokens("alice");

        AuthResponse res = authService.login(new LoginRequest("alice", "secret123"));

        assertThat(res.accessToken()).isEqualTo("access");
        verify(loginAttemptService).recordSuccess("alice");
        verify(loginEventService).record("alice", LoginOutcome.SUCCESS);
    }

    @Test
    void login_saiMatKhau_tangDem_ghiFailure_vaNemLai() {
        when(loginAttemptService.isBlocked("alice")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("sai"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(loginAttemptService).recordFailure("alice");
        verify(loginEventService).record("alice", LoginOutcome.FAILURE);
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void login_dangBiKhoa_tuChoiSom_khongThuXacThuc() {
        when(loginAttemptService.isBlocked("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "secret123")))
                .isInstanceOf(TooManyAttemptsException.class);
        verify(authenticationManager, never()).authenticate(any());
        verify(loginEventService).record("alice", LoginOutcome.BLOCKED);
    }

    // ---- refresh ----

    @Test
    void refresh_hopLe_xoayVongToken() {
        User alice = customer("alice");
        RefreshToken rt = new RefreshToken("hash", alice, Instant.now().plusSeconds(60));
        when(refreshTokenService.verifyUsable("old")).thenReturn(rt);
        UserDetails details = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtService.generateAccessToken(details)).thenReturn("new-access");
        when(refreshTokenService.rotate(rt)).thenReturn("new-refresh");

        AuthResponse res = authService.refresh("old");

        assertThat(res.accessToken()).isEqualTo("new-access");
        assertThat(res.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).rotate(rt);
    }

    @Test
    void refresh_taiSuDungTokenDaThuHoi_thuHoiTatCa_va401() {
        User alice = customer("alice");
        when(refreshTokenService.verifyUsable("stolen")).thenThrow(new RefreshTokenReuseException(alice));

        assertThatThrownBy(() -> authService.refresh("stolen"))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenService).revokeAllForUser(alice);
        verify(refreshTokenService, never()).rotate(any());
    }

    // ---- change password / logout ----

    @Test
    void doiMatKhau_dungMatKhauCu_bamMoi_thuHoiTatCaToken() {
        User alice = customer("alice");
        loginAs("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("old", "{hash}")).thenReturn(true);
        when(passwordEncoder.encode("newSecret1")).thenReturn("{bcrypt}new");

        authService.changePassword("old", "newSecret1");

        assertThat(alice.getPassword()).isEqualTo("{bcrypt}new");
        verify(refreshTokenService).revokeAllForUser(alice);
    }

    @Test
    void doiMatKhau_saiMatKhauCu_khongDoi() {
        User alice = customer("alice");
        loginAs("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("wrong", "{hash}")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("wrong", "newSecret1"))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(alice.getPassword()).isEqualTo("{hash}");
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void logoutAll_thuHoiTokenCuaUserHienTai() {
        User alice = customer("alice");
        loginAs("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        authService.logoutAll();

        verify(refreshTokenService).revokeAllForUser(alice);
    }
}
