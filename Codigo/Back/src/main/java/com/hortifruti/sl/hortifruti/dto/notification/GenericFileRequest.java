package com.hortifruti.sl.hortifruti.dto.notification;

public record GenericFileRequest(
    String fileName,
    String fileType,
    byte[] fileContent) {}