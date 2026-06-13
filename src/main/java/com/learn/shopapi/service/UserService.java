package com.learn.shopapi.service;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.UserResponse;
import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.RoleRepository;
import com.learn.shopapi.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Quan ly user danh cho ADMIN: xem danh sach, gan vai tro, khoa/mo tai khoan.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    @Transactional
    public UserResponse setRoles(Long userId, Set<String> roleNames) {
        User user = getUserOrThrow(userId);
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay role: " + name)));
        }
        user.setRoles(roles);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = getUserOrThrow(userId);
        user.setEnabled(enabled);
        return UserResponse.from(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user id=" + id));
    }
}
