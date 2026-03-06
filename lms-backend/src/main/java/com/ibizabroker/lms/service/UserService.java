package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.RoleRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.ChangePasswordRequest;
import com.ibizabroker.lms.dto.UserCreateDto;
import com.ibizabroker.lms.dto.UserDto;
import com.ibizabroker.lms.dto.UserUpdateDto;
import com.ibizabroker.lms.entity.Role;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Users getUserById(Integer id) {
        return usersRepository.findByIdWithRoles(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    public UserDto createUser(UserCreateDto userCreateDto) {
        if (usersRepository.existsByUsernameIgnoreCase(userCreateDto.getUsername())) {
            throw new IllegalStateException("Username '" + userCreateDto.getUsername() + "' is already taken.");
        }

        Users user = new Users();
        user.setName(userCreateDto.getName());
        user.setUsername(userCreateDto.getUsername().toLowerCase());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(passwordEncoder.encode(userCreateDto.getPassword()));

        Set<Role> roles = mapRoles(userCreateDto.getRoles());
        user.setRoles(roles);

        Users savedUser = usersRepository.save(user);
        return mapToUserDto(savedUser);
    }

    public Users updateUser(Integer id, UserUpdateDto userDetails) {
        Users user = getUserById(id); // Tái sử dụng hàm findById

        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());

        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getStudentClass() != null) {
            user.setStudentClass(userDetails.getStudentClass());
        }
        if (userDetails.getPhoneNumber() != null) {
            user.setPhoneNumber(userDetails.getPhoneNumber());
        }

        if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
            Set<Role> newRoles = mapRoles(new HashSet<>(userDetails.getRoles()));
            user.setRoles(newRoles);
        }

        return usersRepository.save(user);
    }

    @SuppressWarnings("null")
    public void deleteUser(Integer id) {
        Users user = getUserById(id);
        usersRepository.delete(user);
    }

    public Map<String, String> resetPassword(Integer id) {
        Users user = getUserById(id);
        String newPassword = RandomStringUtils.randomAlphanumeric(10);
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
        return Map.of("newPassword", newPassword);
    }

    public void changeOwnPassword(Users user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usersRepository.save(user);
    }

    // --- Helper Methods ---
    public UserDto mapToUserDto(Users user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setStudentClass(user.getStudentClass());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatarUrl(user.getAvatar());
        dto.setActive(user.getActive());
        dto.setRoles(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList()));
        return dto;
    }

    public Users updateUserStatus(Integer id, Boolean active) {
        Users user = getUserById(id);
        user.setActive(active);
        return usersRepository.save(user);
    }

    @SuppressWarnings("null")
    public Users saveUser(Users user) {
        return usersRepository.save(user);
    }

    private Set<Role> mapRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(roleRepository.findByRoleName("ROLE_USER")
                    .orElseThrow(() -> new NotFoundException("Default role ROLE_USER not found.")));
        }
        
        return roleNames.stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
    }
}