package com.car.rental.service;

import java.sql.SQLException;

/** Indicates the database operation was rolled back and its failure event was attempted. */
public class AuditedOperationException extends SQLException {
    public AuditedOperationException(SQLException cause) {
        super(cause.getMessage(), cause.getSQLState(), cause.getErrorCode(), cause);
    }
}
