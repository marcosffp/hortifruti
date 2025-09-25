package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.FreightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FreightConfigRepository extends JpaRepository<FreightConfig, Long> {}
