package com.car.rental.api.dto;

public class HealthResponse {

    private String status;
    private String application;
    private boolean uiEnabled;

    public HealthResponse() {
    }

    public HealthResponse(String status, String application, boolean uiEnabled) {
        this.status = status;
        this.application = application;
        this.uiEnabled = uiEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public boolean isUiEnabled() {
        return uiEnabled;
    }

    public void setUiEnabled(boolean uiEnabled) {
        this.uiEnabled = uiEnabled;
    }
}
