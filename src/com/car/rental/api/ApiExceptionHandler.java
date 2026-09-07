package com.car.rental.api;

import com.car.rental.api.dto.ApiError;
import com.car.rental.service.FingerprintException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = Logger.getLogger(ApiExceptionHandler.class.getName());

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), 400));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiError> sql(SQLException ex) {
        log.log(Level.WARNING, "API SQL error", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getMessage(), 409));
    }

    @ExceptionHandler(FingerprintException.class)
    public ResponseEntity<ApiError> fingerprint(FingerprintException ex) {
        log.log(Level.INFO, "Fingerprint API: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError(ex.getMessage(), 504));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(msg, code));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        log.log(Level.SEVERE, "API unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", 500));
    }
}
