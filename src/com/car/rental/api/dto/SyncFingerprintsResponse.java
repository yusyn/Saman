package com.car.rental.api.dto;

public class SyncFingerprintsResponse {
    private int scannedEmployees;
    private int addedFingers;
    private String message;

    public SyncFingerprintsResponse() {
    }

    public SyncFingerprintsResponse(int scannedEmployees, int addedFingers, String message) {
        this.scannedEmployees = scannedEmployees;
        this.addedFingers = addedFingers;
        this.message = message;
    }

    public int getScannedEmployees() {
        return scannedEmployees;
    }

    public void setScannedEmployees(int scannedEmployees) {
        this.scannedEmployees = scannedEmployees;
    }

    public int getAddedFingers() {
        return addedFingers;
    }

    public void setAddedFingers(int addedFingers) {
        this.addedFingers = addedFingers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
