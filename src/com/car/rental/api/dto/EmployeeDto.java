package com.car.rental.api.dto;

import com.car.rental.model.Employee;

public class EmployeeDto {

    private String deviceUserId;
    private String name;
    private String phone;
    private boolean renting;

    public EmployeeDto() {
    }

    public EmployeeDto(String deviceUserId, String name, String phone, boolean renting) {
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.phone = phone;
        this.renting = renting;
    }

    public static EmployeeDto from(Employee e) {
        if (e == null) {
            return null;
        }
        return new EmployeeDto(e.getDeviceUserId(), e.getName(), e.getPhone(), e.isRenting());
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isRenting() {
        return renting;
    }

    public void setRenting(boolean renting) {
        this.renting = renting;
    }
}
