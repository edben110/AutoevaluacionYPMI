package com.sem.pmiautoevaluacion.review.dto;

import java.util.List;

public record DeliveryDetailResponse(DeliveryResponse delivery, List<ReviewedComponentResponse> components) {}
