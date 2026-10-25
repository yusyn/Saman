package com.car.rental.model;

public record SystemEvent(long id, String occurredAt, String category, String eventType,
                          String result, String description, String actor,
                          String entityType, String entityId, String detailsJson) { }
