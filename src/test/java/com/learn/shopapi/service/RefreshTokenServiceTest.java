package com.learn.shopapi.service;

import com.learn.shopapi.entity.RefreshToken;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.RefreshTokenReuseException;
import com.learn.shopapi.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test RefreshTokenService: chi luu ban bam, kiem tra het han / thu hoi, xoay vong. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long TTL_MS = 7L * 24 * 3600 * 1000;

    @Mock private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private final User alice = new User("alice", "{hash}", "alice@mail.vn", null);

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, TTL_MS);
    }

    private static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void create_traTokenGoc_nhungChiLuuBanBam() throws Exception {
        String raw = service.create(alice);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo(sha256(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getExpiryDate())
                .isCloseTo(Instant.now().plusMillis(TTL_MS), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void verify_hopLe_traEntity() throws Exception {
        RefreshToken rt = new RefreshToken(sha256("raw"), alice, Instant.now().plusSeconds(60));
        when(repository.findByToken(sha256("raw"))).thenReturn(Optional.of(rt));

        assertThat(service.verifyUsable("raw")).isSameAs(rt);
    }

    @Test
    void verify_khongTonTai_401() {
        when(repository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyUsable("nope")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void verify_hetHan_401() throws Exception {
        RefreshToken rt = new RefreshToken(sha256("raw"), alice, Instant.now().minusSeconds(1));
        when(repository.findByToken(sha256("raw"))).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> service.verifyUsable("raw"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("het han");
    }

    @Test
    void verify_daThuHoi_baoTaiSuDungKemUser() throws Exception {
        RefreshToken rt = new RefreshToken(sha256("raw"), alice, Instant.now().plusSeconds(60));
        rt.setRevoked(true);
        when(repository.findByToken(sha256("raw"))).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> service.verifyUsable("raw"))
                .isInstanceOfSatisfying(RefreshTokenReuseException.class, e -> assertThat(e.getUser()).isSameAs(alice));
    }

    @Test
    void rotate_thuHoiCu_capMoi() {
        RefreshToken current = new RefreshToken("old-hash", alice, Instant.now().plusSeconds(60));

        String fresh = service.rotate(current);

        assertThat(current.isRevoked()).isTrue();
        assertThat(fresh).isNotBlank();
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void revoke_tokenTonTai_danhDauThuHoi() throws Exception {
        RefreshToken rt = new RefreshToken(sha256("raw"), alice, Instant.now().plusSeconds(60));
        when(repository.findByToken(sha256("raw"))).thenReturn(Optional.of(rt));

        service.revoke("raw");

        assertThat(rt.isRevoked()).isTrue();
    }
}
