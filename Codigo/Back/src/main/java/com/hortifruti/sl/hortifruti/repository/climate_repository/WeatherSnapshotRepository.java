package com.hortifruti.sl.hortifruti.repository.climate_repository;

import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherSnapshotRepository extends JpaRepository<WeatherSnapshot, Long> {
    Optional<WeatherSnapshot> findByDate(LocalDate date);
    List<WeatherSnapshot> findByDateBetween(LocalDate start, LocalDate end);
}
