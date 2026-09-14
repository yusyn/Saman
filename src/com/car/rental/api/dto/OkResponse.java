package com.car.rental.api.dto;

public class OkResponse {

    private boolean ok;
    private String message;

    public OkResponse() {
    }

    public OkResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static OkResponse ok(String message) {
        return new OkResponse(true, message);
    }

    public static OkResponse fail(String message) {
        return new OkResponse(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
