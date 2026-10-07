package com.sem.pmiautoevaluacion.integralManagement.entity;
import com.sem.pmiautoevaluacion.shared.enums.UseState;

import jakarta.persistence.*;

@Entity 
@Table (name = "component")
public class Component extends IntegralManagement{
    @Column(name = "expected_evidence", columnDefinition = "TEXT")
    private String expectedEvidence;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "process_id", nullable = false)
    private Process process;

    protected Component(){
        super();
    }

    public Component(String name, String description, String expectedEvidence){
        super(name,description);
        this.expectedEvidence = expectedEvidence;
    }

    public String getExpectedEvidence() {
        return expectedEvidence;
    }

    public void setExpectedEvidence(String expectedEvidence) {
        this.expectedEvidence = expectedEvidence;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public void deactivate() {
        this.setState(UseState.INACTIVE);
    }

    public void activate() {
        this.setState(UseState.ACTIVE);
    }
}
