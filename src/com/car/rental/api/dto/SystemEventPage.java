package com.car.rental.api.dto;

import com.car.rental.model.SystemEvent;
import java.util.List;

public record SystemEventPage(List<SystemEvent> items, int page, int pageSize, long total) { }
