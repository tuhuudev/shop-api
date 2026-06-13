package com.learn.shopapi.security;

import com.learn.shopapi.entity.Permission;
import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Cau noi giua User (entity cua ta) va UserDetails (kieu Spring Security hieu).
 *
 * Spring Security goi loadUserByUsername(...) luc dang nhap. Ta nap User tu DB roi
 * doi cac Role/Permission thanh "authority":
 *   - moi Role -> "ROLE_<ten>" (vi du ADMIN -> ROLE_ADMIN). hasRole("ADMIN") se khop cai nay.
 *   - moi Permission -> giu nguyen ten (vi du "PRODUCT_WRITE"). hasAuthority("PRODUCT_WRITE") khop.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        // Dung lop User co san cua Spring Security lam UserDetails.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
    }
}
