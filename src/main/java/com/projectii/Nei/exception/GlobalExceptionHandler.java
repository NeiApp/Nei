package com.projectii.Nei.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ActiveWeekException.class)
    public ResponseEntity<Map<String,String>> handleActiveWeekException(ActiveWeekException ex){
        Map<String,String> error = new HashMap<>();
        error.put("message", "active week was not found");
        return ResponseEntity.badRequest().body(error);
    }
}
