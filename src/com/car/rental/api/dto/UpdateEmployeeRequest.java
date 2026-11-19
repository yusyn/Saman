package com.car.rental.api.dto;

/**
 * Update employee name/phone. deviceUserId is taken from the path.
 */
public class UpdateEmployeeRequest {

    private String name;
    private String phone;
    private String driverLicenseNumber;
    private String driverLicenseExpiry;

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
