package com.sem.pmiautoevaluacion.integralManagement.entity;
import java.util.ArrayList;
import java.util.List;

import com.sem.pmiautoevaluacion.shared.enums.UseState;

import jakarta.persistence.*;

@Entity 
@Table (name = "area")
public class Area extends IntegralManagement{
    @OneToMany (mappedBy = "area", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Process> processes = new ArrayList<>();

    protected Area(){
        super();
    }

    public Area(
        String name,
        String description
    ){
        super(name,description);
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }

    // Metodos de ayuda para mantener ambos lados sincronizados
    public void addProcess(Process process){
        processes.add(process);
        process.setArea(this);
    }

    public void removeProcess(Process process){
        processes.remove(process);
        process.setArea(null);
    }

    public void deactivate() {
        this.setState(UseState.INACTIVE);
        processes.forEach(Process::deactivate);
    }

    public void activate() {
        this.setState(UseState.ACTIVE);
    }
}
