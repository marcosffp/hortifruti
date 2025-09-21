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
    user.setPassword(passwordEncoder.encode(userRequest.password()));
    user.setRole(userRequest.role());
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
