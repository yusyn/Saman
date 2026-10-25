package com.car.rental.api;

import com.car.rental.api.dto.ApiError;
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
import com.car.rental.service.SystemEventService;
import com.car.rental.service.AuditedOperationException;
import com.car.rental.service.DeviceOperationException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = Logger.getLogger(ApiExceptionHandler.class.getName());
    private final SystemEventService systemEvents;
    public ApiExceptionHandler(SystemEventService systemEvents) { this.systemEvents = systemEvents; }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        systemEvents.recordFailureSafely(new SystemEventService.EventSpec("API", "درخواست نامعتبر", "یک درخواست API به‌دلیل ورودی نامعتبر رد شد", "API", null), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), 400));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiError> sql(SQLException ex) {
        if (!(ex instanceof AuditedOperationException)) {
            systemEvents.recordFailureSafely(new SystemEventService.EventSpec("API", "خطای پایگاه‌داده", "یک درخواست API با خطای پایگاه‌داده پایان یافت", "API", null), ex);
        }
        // Log full detail server-side only; never return raw SQL / table / constraint text.
        log.log(Level.WARNING, "API SQL error", ex);
        String clientMessage = mapSqlClientMessage(ex);
        HttpStatus status = clientMessage.contains("وجود دارد") || clientMessage.contains("قبلاً")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiError(clientMessage, status.value()));
    }

    /**
     * Prefer known Persian domain messages already set on SQLException; otherwise generic.
     */
    private static String mapSqlClientMessage(SQLException ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "خطای پایگاه داده";
        }
        // Domain messages from repositories (Persian, no schema leakage)
        if (msg.contains("قبلاً") || msg.contains("وجود دارد") || msg.contains("یافت نشد")
                || msg.contains("اجاره") || msg.contains("فعال") || msg.contains("حذف")) {
            // Still avoid dumping multi-line JDBC internals if mixed
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

    @ExceptionHandler(FingerprintException.class)
    public ResponseEntity<ApiError> fingerprint(FingerprintException ex) {
        systemEvents.recordFailureSafely(new SystemEventService.EventSpec("دستگاه اثرانگشت", "خطای عملیات دستگاه", "عملیات دستگاه اثرانگشت ناموفق بود", "دستگاه", null), ex);
        log.log(Level.INFO, "Fingerprint API: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError(ex.getMessage(), 504));
    }

    @ExceptionHandler(DeviceOperationException.class)
    public ResponseEntity<Map<String,Object>> deviceOperation(DeviceOperationException ex) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("message",ex.getMessage());
        body.put("operationId",ex.getOperationId());
        body.put("operationState",ex.getState());
        body.put("deviceSucceeded",ex.getDeviceSucceeded());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException ex) {
        systemEvents.recordFailureSafely(new SystemEventService.EventSpec("API", "خطای درخواست", "درخواست API با وضعیت " + ex.getStatusCode().value() + " رد شد", "API", null), ex);
        int code = ex.getStatusCode().value();
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(msg, code));
    }

    /**
     * Browser auto-requests like {@code /favicon.ico} must not spam ERROR logs.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> missingStatic(NoResourceFoundException ex) {
        log.fine("Static resource not found: " + ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        systemEvents.recordFailureSafely(new SystemEventService.EventSpec("API", "خطای API", "در پردازش درخواست خطای پیش‌بینی‌نشده رخ داد", "API", null), ex);
        log.log(Level.SEVERE, "API unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", 500));
    }
}
