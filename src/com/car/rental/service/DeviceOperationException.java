package com.car.rental.service;

public class DeviceOperationException extends RuntimeException {
    private final String operationId;
    private final String state;
    private final int httpStatus;
    private final Boolean deviceSucceeded;

    public DeviceOperationException(String message, String operationId, String state, int httpStatus, Throwable cause) {
        this(message, operationId, state, httpStatus, null, cause);
    }
    public DeviceOperationException(String message, String operationId, String state, int httpStatus, Boolean deviceSucceeded, Throwable cause) {
        super(message, cause); this.operationId=operationId; this.state=state; this.httpStatus=httpStatus; this.deviceSucceeded=deviceSucceeded;
    }
    public DeviceOperationException(String message, String operationId, String state, int httpStatus) {
        this(message,operationId,state,httpStatus,null);
    }
    public String getOperationId(){return operationId;}
    public String getState(){return state;}
    public int getHttpStatus(){return httpStatus;}
    public Boolean getDeviceSucceeded(){return deviceSucceeded;}
}
