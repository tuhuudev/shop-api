package com.learn.shopapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JwtService = tao va kiem tra JWT bang RS256 (RSA bat doi xung).
 *
 * - Ky bang PRIVATE key, verify bang PUBLIC key -> dich vu khac (gateway/microservice) co the
 *   verify token chi voi public key ma KHONG can biet private key. An toan hon HS256 (khoa chung).
 * - Tat ca instance dung CHUNG cap khoa (nap tu file/secret) -> token cua instance nay,
 *   instance khac verify duoc (multi-instance). KHONG sinh khoa ngau nhien luc chay.
 * - Khoa dev nam o classpath (keys/*.pem); production tro toi secret qua bien moi truong.
 */
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.private-key}") Resource privateKeyResource,
            @Value("${app.jwt.public-key}") Resource publicKeyResource,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        this.privateKey = loadPrivateKey(privateKeyResource);
        this.publicKey = loadPublicKey(publicKeyResource);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.issuer = issuer;
    }

    /** Tao access token tu thong tin user (username + cac quyen), ky bang private key (RS256). */
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
                .signWith(privateKey)   // RSA key -> jjwt dung RS256
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            return parseClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)        // verify bang public key
                .requireIssuer(issuer)        // tu choi token khong phai do "shop-api" phat hanh
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ---- nap khoa tu PEM ----

    private PrivateKey loadPrivateKey(Resource resource) {
        byte[] der = pemToDer(read(resource), "PRIVATE KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Khong doc duoc JWT private key", e);
        }
    }

    private PublicKey loadPublicKey(Resource resource) {
        byte[] der = pemToDer(read(resource), "PUBLIC KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Khong doc duoc JWT public key", e);
        }
    }

    private String read(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Khong mo duoc file khoa JWT: " + resource, e);
        }
    }

    /** Bo header/footer/khoang trang cua PEM va base64-decode ra DER. */
    private byte[] pemToDer(String pem, String type) {
        String body = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
