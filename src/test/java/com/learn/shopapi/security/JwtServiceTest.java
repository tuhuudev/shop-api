package com.learn.shopapi.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test JwtService (RS256): sinh cap khoa RSA tam trong test, khong can file keys/*.pem. */
class JwtServiceTest {

    private static KeyPair keys;
    private static KeyPair otherKeys;

    private static final UserDetails ALICE = User.withUsername("alice")
            .password("x")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build();

    @BeforeAll
    static void genKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keys = gen.generateKeyPair();
        otherKeys = gen.generateKeyPair();
    }

    private static Resource pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der);
        return new ByteArrayResource(
                ("-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n").getBytes(StandardCharsets.UTF_8));
    }

    private static JwtService service(long ttlMs, String issuer) {
        return new JwtService(
                pem("PRIVATE KEY", keys.getPrivate().getEncoded()),
                pem("PUBLIC KEY", keys.getPublic().getEncoded()),
                ttlMs, issuer);
    }

    @Test
    void taoVaDocLaiToken() {
        JwtService jwt = service(60_000, "shop-api");
        String token = jwt.generateAccessToken(ALICE);

        assertThat(jwt.isTokenValid(token)).isTrue();
        assertThat(jwt.extractUsername(token)).isEqualTo("alice");
        assertThat(Jwts.parser().verifyWith(keys.getPublic()).build().parseSignedClaims(token)
                .getPayload().get("authorities", List.class))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void tokenHetHan_khongHopLe() {
        JwtService jwt = service(-1_000, "shop-api");
        assertThat(jwt.isTokenValid(jwt.generateAccessToken(ALICE))).isFalse();
    }

    @Test
    void issuerKhac_khongHopLe() {
        String foreign = service(60_000, "other-service").generateAccessToken(ALICE);
        assertThat(service(60_000, "shop-api").isTokenValid(foreign)).isFalse();
    }

    @Test
    void kyBangKhoaKhac_khongHopLe() {
        String forged = Jwts.builder().issuer("shop-api").subject("admin")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKeys.getPrivate()).compact();
        assertThat(service(60_000, "shop-api").isTokenValid(forged)).isFalse();
    }

    @Test
    void tokenKhongKy_algNone_khongHopLe() {
        String unsigned = Jwts.builder().issuer("shop-api").subject("admin")
                .expiration(new Date(System.currentTimeMillis() + 60_000)).compact();
        assertThat(service(60_000, "shop-api").isTokenValid(unsigned)).isFalse();
    }

    @Test
    void suaPayload_khongHopLe() {
        JwtService jwt = service(60_000, "shop-api");
        String[] parts = jwt.generateAccessToken(ALICE).split("\\.");
        String evil = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"shop-api\",\"sub\":\"admin\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
        assertThat(jwt.isTokenValid(parts[0] + "." + evil + "." + parts[2])).isFalse();
        assertThat(jwt.isTokenValid("rac")).isFalse();
    }
}
