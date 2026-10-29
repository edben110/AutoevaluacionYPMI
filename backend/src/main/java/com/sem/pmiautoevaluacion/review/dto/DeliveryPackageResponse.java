package com.sem.pmiautoevaluacion.review.dto;

import java.time.Instant;
import java.util.*;

public record DeliveryPackageResponse(UUID establishmentId, String establishmentName, String daneCode,
        int year, List<DeliveryResponse> areas, int expectedAreas, boolean reviewed,
        boolean changedSinceReview, Instant reviewedAt, String reviewedBy, String version) {}
