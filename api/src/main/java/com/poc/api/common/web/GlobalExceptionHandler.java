package com.poc.api.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TelemetryValidationException.class)
  public ResponseEntity<Map<String, Object>> handleTelemetryValidation(TelemetryValidationException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
