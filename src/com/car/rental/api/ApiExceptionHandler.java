package com.car.rental.api;

import com.car.rental.api.dto.ApiError;
import com.car.rental.service.FingerprintDeviceBusyException;
import com.car.rental.service.FingerprintDeviceUnavailableException;
import com.car.rental.service.FingerprintException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        String clientMessage = mapSqlClientMessage(ex);
        HttpStatus status = clientMessage.contains("وجود دارد") || clientMessage.contains("قبلاً")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiError(clientMessage, status.value()));
    }

    private static String mapSqlClientMessage(SQLException ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "خطای پایگاه داده";
        }
        if (msg.contains("قبلاً") || msg.contains("وجود دارد") || msg.contains("یافت نشد")
                || msg.contains("اجاره") || msg.contains("فعال") || msg.contains("حذف")) {
            String first = msg.split("[\r\n]")[0].strip();
            if (first.length() <= 200 && !first.toLowerCase().contains("sqlite")
                    && !first.toLowerCase().contains("table")
                    && !first.contains("CONSTRAINT")
                    && !first.contains("SQLException")) {
                return first;
            }
        }
        return "خطای پایگاه داده";
    }

    @ExceptionHandler(FingerprintDeviceUnavailableException.class)
    public ResponseEntity<ApiError> fingerprintUnavailable(FingerprintDeviceUnavailableException ex) {
        log.log(Level.INFO, "Fingerprint device unavailable: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(FingerprintDeviceUnavailableException.CLIENT_MESSAGE, 503));
    }

    @ExceptionHandler(FingerprintDeviceBusyException.class)
    public ResponseEntity<ApiError> fingerprintBusy(FingerprintDeviceBusyException ex) {
        log.log(Level.INFO, "Fingerprint device busy: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(FingerprintDeviceBusyException.CLIENT_MESSAGE, 409));
    }

    @ExceptionHandler(FingerprintException.class)
    public ResponseEntity<ApiError> fingerprint(FingerprintException ex) {
        log.log(Level.INFO, "Fingerprint API: " + ex.getMessage());
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "خطای دستگاه اثر انگشت";
        }
        String lower = msg.toLowerCase();
        if (lower.contains("192.") || lower.contains("socket") || lower.contains("timed out")
                || lower.contains("connection refused") || lower.contains("cannot connect")
                || (msg.contains(":") && msg.matches(".*\\d{1,5}.*"))) {
            msg = "خطای ارتباط با دستگاه اثر انگشت";
        }
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError(msg, 504));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(msg, code));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> missingStatic(NoResourceFoundException ex) {
        log.fine("Static resource not found: " + ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        log.log(Level.SEVERE, "API unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", 500));
    }
}
