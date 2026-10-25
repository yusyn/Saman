package com.car.rental.model;

public record DeviceOperation(String operationId, String operationType, String entityType, String entityId,
                              String actor, String state, Boolean deviceSucceeded, String note,
                              String detailsJson, String createdAt, String updatedAt) { }
