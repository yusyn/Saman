package com.car.rental.api.dto;

import com.car.rental.model.DeviceOperation;
import java.util.List;

public record DeviceOperationPage(List<DeviceOperation> items,int total) { }
