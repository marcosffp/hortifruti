package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.dto.user.UserRequest;
import com.hortifruti.sl.hortifruti.dto.user.UserResponse;
import com.hortifruti.sl.hortifruti.dto.user.UsersCountResponse;
import com.hortifruti.sl.hortifruti.exception.UserException;
import com.hortifruti.sl.hortifruti.mapper.UserMapper;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserResponse saveUser(UserRequest userRequest) {
    User user = userMapper.toUser(userRequest);
    user.setPassword(passwordEncoder.encode(userRequest.password()));
    User savedUser = userRepository.save(user);
    return userMapper.toUserResponse(savedUser);
  }

  public UserResponse updateUser(UserRequest userRequest) {
    User user = userRepository.findByUsername(userRequest.username());
    if (user == null) {
      throw new UserException("Usuário não encontrado");
    }

    if (userRequest.password() != null && !userRequest.password().trim().isEmpty()) {
      String password = userRequest.password().trim();
      if (password.length() < 4 || password.length() > 20) {
        throw new UserException("A senha deve ter entre 4 e 20 caracteres");
      }
      user.setPassword(passwordEncoder.encode(password));
    }

    user.setRole(userRequest.role());
    User updatedUser = userRepository.save(user);
    return userMapper.toUserResponse(updatedUser);
  }

  public UserResponse updateUserById(Long id, UserRequest userRequest) {
    User user = userRepository.findById(id).orElse(null);
    if (user == null) {
      throw new UserException("Usuário não encontrado");
    }

    if (userRequest.username() != null && !userRequest.username().trim().isEmpty()) {
      User existingUser = userRepository.findByUsername(userRequest.username());
      if (existingUser != null && !existingUser.getId().equals(id)) {
        throw new UserException("Este nome já está sendo usado por outro usuário");
      }
      user.setUsername(userRequest.username());
    }

    if (userRequest.password() != null) {
      String trimmedPassword = userRequest.password().trim();
      if (!trimmedPassword.isEmpty()) {
        if (trimmedPassword.length() < 4 || trimmedPassword.length() > 20) {
          throw new UserException("A senha deve ter entre 4 e 20 caracteres");
        }
        user.setPassword(passwordEncoder.encode(trimmedPassword));
      }
    }

    if (userRequest.role() != null) {
      user.setRole(userRequest.role());
    }

    User updatedUser = userRepository.save(user);
    return userMapper.toUserResponse(updatedUser);
  }

  public void deleteUser(String username) {
    User user = userRepository.findByUsername(username);
    if (user == null) throw new UserException("Usuário não encontrado");
    userRepository.deleteById(user.getId());
  }

  public List<UserResponse> getUsersByRole(Role role) {
    return userRepository.findByRole(role).stream().map(userMapper::toUserResponse).toList();
  }

  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
  }

  public UsersCountResponse getUsersCountResponse() {
    UsersCountResponse user =
        new UsersCountResponse(
            userRepository.getUsersCount(),
            userRepository.getUsersCountByRole(Role.MANAGER),
            userRepository.getUsersCountByRole(Role.EMPLOYEE));
    return user;
  }
}
