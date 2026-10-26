package com.sem.pmiautoevaluacion.pmi.repository;

import com.sem.pmiautoevaluacion.pmi.entity.PMIFollowUp;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PMIFollowUpRepository extends JpaRepository<PMIFollowUp, UUID> {
    List<PMIFollowUp> findByPmi_Id(UUID pmiId);
    List<PMIFollowUp> findByPmi_IdAndControlDateOrderByIdAsc(UUID pmiId, LocalDate controlDate);
    Optional<PMIFollowUp> findByPmi_IdAndControlDateAndSourceRowId(UUID pmiId, LocalDate controlDate, UUID sourceRowId);
    @Query("select distinct f.controlDate from PMIFollowUp f where f.pmi.id = :pmiId order by f.controlDate desc")
    List<LocalDate> findControlDates(UUID pmiId);
}
