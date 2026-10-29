package com.sem.pmiautoevaluacion.pmi.repository;

import com.sem.pmiautoevaluacion.pmi.entity.PMIFollowUpCompletion;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PMIFollowUpCompletionRepository extends JpaRepository<PMIFollowUpCompletion, UUID> {
    Optional<PMIFollowUpCompletion> findByPmi_Id(UUID pmiId);
}
