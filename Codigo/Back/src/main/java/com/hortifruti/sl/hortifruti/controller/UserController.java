package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.UserRequest;
import com.hortifruti.sl.hortifruti.dto.UserResponse;
import com.hortifruti.sl.hortifruti.dto.UsersCountResponse;
import com.hortifruti.sl.hortifruti.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

  private final UserService userService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/register")
  public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRequest userRequest) {
    return ResponseEntity.ok(userService.saveUser(userRequest));
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/all")
  public ResponseEntity<List<UserResponse>> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PutMapping("/update")
  public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UserRequest userRequest) {
    return ResponseEntity.ok(userService.updateUser(userRequest));
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PutMapping("/update/{id}")
  public ResponseEntity<UserResponse> updateUserById(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest) {
    return ResponseEntity.ok(userService.updateUserById(id, userRequest));
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/count")
  public ResponseEntity<UsersCountResponse> getUsersCount() {
    return ResponseEntity.ok(userService.getUsersCountResponse());
  }

  @PreAuthorize("hasRole('MANAGER')")
  @DeleteMapping("/delete/{username}")
  public ResponseEntity<Void> deleteUser(@PathVariable String username) {
    userService.deleteUser(username);
    return ResponseEntity.noContent().build();
  }
}
