package com.hortifruti.sl.hortifruti.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.hortifruti.sl.hortifruti.model.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    
    Optional<Client> findByEmail(String email);
}
