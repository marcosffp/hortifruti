package com.hortifruti.sl.hortifruti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiTokenService {

    @Value("${api.token.scheduler}")
    private String schedulerToken;

    /**
     * Valida se o token fornecido é válido para acesso aos endpoints do scheduler
     * 
     * @param token O token a ser validado
     * @return true se o token for válido, false caso contrário
     */
    public boolean validateSchedulerToken(String token) {
        return schedulerToken != null && schedulerToken.equals(token);
    }
}