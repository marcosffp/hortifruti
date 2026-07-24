package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.LoginAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {}
