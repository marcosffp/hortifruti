package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.config.TokenConfiguration;
import com.hortifruti.sl.hortifruti.dto.UserRequest;
import com.hortifruti.sl.hortifruti.dto.UserResponse;
import com.hortifruti.sl.hortifruti.mapper.UserMapper;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final TokenConfiguration tokenConfiguration;

  public Map<String, UserResponse> saveUser(UserRequest userRequest) {
    User user = userMapper.toUser(userRequest);
    user.setPassword(passwordEncoder.encode(userRequest.password()));
    User savedUser = userRepository.save(user);
    String token =
        tokenConfiguration.generateToken(
            savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    return Map.of(token, userMapper.toUserResponse(savedUser));
  }

  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
  }
}
