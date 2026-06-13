package com.learn.shopapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * JwtService = "long" tao va kiem tra JWT (JSON Web Token).
 *
 * JWT gom 3 phan ngan cach dau cham: header.payload.chu_ky
 * - payload chua "claims": subject (username), danh sach quyen, thoi diem het han...
 * - chu_ky duoc ky bang khoa bi mat (HS256). Doi 1 ky tu -> chu_ky sai -> token bi tu choi.
 *   Nho vay server KHONG can luu phien dang nhap (stateless): chi can verify chu_ky.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        // secret duoc luu dang base64 trong application.properties -> giai ma ra byte de tao khoa.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.issuer = issuer;
    }

    /** Tao access token tu thong tin user (username + cac quyen). */
    public String generateAccessToken(UserDetails user) {
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Doc username (subject) tu token. Nem exception neu token sai/het han. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** True neu token hop le (chu ky dung) va chua het han. */
    public boolean isTokenValid(String token) {
        try {
            return parseClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)   // tu choi token khong phai do "shop-api" phat hanh
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
