package com.learn.shopapi.security;

import com.learn.shopapi.entity.Permission;
import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Cau noi giua User (entity cua ta) va UserDetails (kieu Spring Security hieu).
 *
 * Spring Security goi loadUserByUsername(...) luc dang nhap; JwtAuthenticationFilter cung goi
 * o MOI request (de lay quyen moi nhat + kiem tra enabled). Vi the day la diem nong ve perf.
 *
 * CACHE (don bay perf, MAC DINH TAT):
 * - app.security.userdetails-cache-seconds=0 -> khong cache: moi request nap lai tu DB
 *   (thu hoi quyen / khoa user co hieu luc TUC THI). Day la mac dinh an toan.
 * - > 0 -> cache UserDetails trong N giay: giam tai DB nhung viec thu hoi/doi quyen
 *   tre toi da N giay. Chi bat khi can throughput cao va chap nhan do tre nay.
 * Nho Hibernate acquire-connection-tre, cache-hit KHONG chiem connection du method @Transactional.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String KEY_PREFIX = "shop:userdetails:";

    private final UserRepository userRepository;
    private final RedisTemplate<String, UserDetails> cache;
    private final long cacheSeconds;   // <= 0: tat cache (nap lai tu DB moi request)

    public CustomUserDetailsService(
            UserRepository userRepository,
            RedisTemplate<String, UserDetails> userDetailsRedisTemplate,
            @Value("${app.security.userdetails-cache-seconds:0}") long cacheSeconds) {
        this.userRepository = userRepository;
        this.cache = userDetailsRedisTemplate;
        this.cacheSeconds = cacheSeconds;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (cacheSeconds > 0) {
            UserDetails cached = cache.opsForValue().get(KEY_PREFIX + username);
            if (cached != null) {
                return cached;
            }
        }
        UserDetails details = loadFromDb(username); // chay trong transaction nay (lazy roles/perms OK)
        if (cacheSeconds > 0) {
            cache.opsForValue().set(KEY_PREFIX + username, details, Duration.ofSeconds(cacheSeconds));
        }
        return details;
    }

    private UserDetails loadFromDb(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
    }
}
