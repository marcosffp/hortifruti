package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  @Query("SELECT u FROM User u WHERE u.username = :username")
  User findByUsername(@Param("username") String username);

  @Query("SELECT u FROM User u WHERE u.role = :role")
  List<User> findByRole(@Param("role") Role role);

  @Query("SELECT COUNT(u) FROM User u")
  long getUsersCount();

  @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
  long getUsersCountByRole(@Param("role") Role role);
}
