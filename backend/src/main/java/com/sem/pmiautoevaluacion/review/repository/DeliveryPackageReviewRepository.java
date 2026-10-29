package com.sem.pmiautoevaluacion.review.repository;

import com.sem.pmiautoevaluacion.review.entity.DeliveryPackageReview;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DeliveryPackageReviewRepository extends JpaRepository<DeliveryPackageReview, UUID> {
    Optional<DeliveryPackageReview> findByEvaluation_Id(UUID evaluationId);
    @EntityGraph(attributePaths = {"evaluation", "evaluation.establishment"})
    @Query("select r from DeliveryPackageReview r")
    List<DeliveryPackageReview> findAllForPackages();
    @EntityGraph(attributePaths = {"evaluation", "evaluation.establishment"})
    List<DeliveryPackageReview> findByEvaluation_Establishment_Id(UUID establishmentId);
}
