package com.learn.shopapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chay 1 lan moi request (OncePerRequestFilter), DUNG TRUOC bo loc dang nhap chuan.
 *
 * Nhiem vu: doc header "Authorization: Bearer <token>", neu token hop le thi tao
 * Authentication va dat vao SecurityContext -> Spring coi nhu nguoi dung da dang nhap.
 *
 * Quan trong (bao mat): sau khi xac thuc chu ky token, ta NAP LAI user tu DB de:
 *   - lay quyen MOI NHAT (neu admin vua doi vai tro -> co hieu luc ngay);
 *   - kiem tra tai khoan con "enabled" khong (neu vua bi khoa -> token het tac dung ngay).
 * Doi lai phai truy DB moi request (chap nhan duoc voi du an nay).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        // Khong co token -> bo qua, de cac filter sau xu ly (vi du endpoint public van chay).
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        if (jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtService.extractUsername(token);
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (user.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // user bi khoa (enabled=false) -> khong xac thuc -> endpoint can quyen se tra 401.
            } catch (UsernameNotFoundException ex) {
                // Tai khoan trong token khong con ton tai -> coi nhu chua dang nhap.
            }
        }

        filterChain.doFilter(request, response);
    }
}
