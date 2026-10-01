package com.sem.pmiautoevaluacion.integralManagement.entity;
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

    public Component(String name, String description){
        super(name,description);
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


}
