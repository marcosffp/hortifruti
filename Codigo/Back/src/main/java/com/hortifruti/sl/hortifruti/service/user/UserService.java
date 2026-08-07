package com.hortifruti.sl.hortifruti.service.user;

import com.hortifruti.sl.hortifruti.dto.user.UserRequest;
import com.hortifruti.sl.hortifruti.dto.user.UserResponse;
import com.hortifruti.sl.hortifruti.dto.user.UserUpdateRequest;
import com.hortifruti.sl.hortifruti.dto.user.UsersCountResponse;
import com.hortifruti.sl.hortifruti.exception.user.UserException;
import com.hortifruti.sl.hortifruti.mapper.UserMapper;
import com.hortifruti.sl.hortifruti.model.Role;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

  private static final int PASSWORD_MIN_LENGTH = 8;
  private static final int PASSWORD_MAX_LENGTH = 72;

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserResponse saveUser(UserRequest userRequest) {
    User user = userMapper.toUser(userRequest);
    user.setPassword(passwordEncoder.encode(userRequest.password()));
    User savedUser = userRepository.save(user);
    return userMapper.toUserResponse(savedUser);
  }

  public UserResponse updateUser(UserUpdateRequest userRequest) {
    User user = userRepository.findByUsername(userRequest.username());
    if (user == null) {
      throw new UserException("Usuário não encontrado");
    }

    if (userRequest.password() != null && !userRequest.password().trim().isEmpty()) {
      user.setPassword(encodeValidatedPassword(userRequest.password()));
    }

    user.setRole(userRequest.role());
    User updatedUser = userRepository.save(user);
    return userMapper.toUserResponse(updatedUser);
  }

  public UserResponse updateUserById(Long id, UserUpdateRequest userRequest) {
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

    if (userRequest.password() != null && !userRequest.password().trim().isEmpty()) {
      user.setPassword(encodeValidatedPassword(userRequest.password()));
    }

    if (userRequest.role() != null) {
      user.setRole(userRequest.role());
    }

    User updatedUser = userRepository.save(user);
    return userMapper.toUserResponse(updatedUser);
  }

  /**
   * Trim + valida o tamanho (NIST 800-63B: mínimo 8; teto em 72 porque é o limite de bytes que o
   * BCrypt considera) antes de codificar. Centralizado aqui porque {@code updateUser} e {@code
   * updateUserById} precisam da mesma regra ao trocar senha.
   */
  private String encodeValidatedPassword(String rawPassword) {
    String password = rawPassword.trim();
    if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
      throw new UserException(
          "A senha deve ter entre "
              + PASSWORD_MIN_LENGTH
              + " e "
              + PASSWORD_MAX_LENGTH
              + " caracteres");
    }
    return passwordEncoder.encode(password);
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
