package com.car.rental.api.dto;

import com.car.rental.model.Employee;

import java.util.ArrayList;
import java.util.List;

public class EmployeeDto {

    private String deviceUserId;
    private String name;
    private String phone;
    private boolean renting;
    private List<Integer> fingers = new ArrayList<>();
    private String driverLicenseNumber;
    private String driverLicenseExpiry;

    public EmployeeDto() {
    }

    public EmployeeDto(String deviceUserId, String name, String phone, boolean renting) {
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.phone = phone;
        this.renting = renting;
    }

    public EmployeeDto(String deviceUserId, String name, String phone, boolean renting, List<Integer> fingers) {
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.phone = phone;
        this.renting = renting;
        this.fingers = fingers != null ? fingers : new ArrayList<>();
    }

    public static EmployeeDto from(Employee e) {
        if (e == null) {
            return null;
        }
        EmployeeDto dto = new EmployeeDto(e.getDeviceUserId(), e.getName(), e.getPhone(), e.isRenting());
        dto.setDriverLicenseNumber(e.getDriverLicenseNumber());
        dto.setDriverLicenseExpiry(e.getDriverLicenseExpiry());
        return dto;
    }

    public static EmployeeDto from(Employee e, List<Integer> fingers) {
        if (e == null) {
            return null;
        }
        EmployeeDto dto = new EmployeeDto(e.getDeviceUserId(), e.getName(), e.getPhone(), e.isRenting(), fingers);
        dto.setDriverLicenseNumber(e.getDriverLicenseNumber());
        dto.setDriverLicenseExpiry(e.getDriverLicenseExpiry());
        return dto;
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

    public List<Integer> getFingers() {
        return fingers;
    }

    public void setFingers(List<Integer> fingers) {
        this.fingers = fingers != null ? fingers : new ArrayList<>();
    }

    public String getDriverLicenseNumber() {
        return driverLicenseNumber;
    }

    public void setDriverLicenseNumber(String driverLicenseNumber) {
        this.driverLicenseNumber = driverLicenseNumber;
    }

    public String getDriverLicenseExpiry() {
        return driverLicenseExpiry;
    }

    public void setDriverLicenseExpiry(String driverLicenseExpiry) {
        this.driverLicenseExpiry = driverLicenseExpiry;
    }
}
