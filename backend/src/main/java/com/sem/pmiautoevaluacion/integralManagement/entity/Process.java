package com.sem.pmiautoevaluacion.integralManagement.entity;

import java.util.ArrayList;
import java.util.List;

import com.sem.pmiautoevaluacion.shared.enums.UseState;

import jakarta.persistence.*;

@Entity 
@Table (name = "process")
public class Process extends IntegralManagement{
    @OneToMany (mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Component> components = new ArrayList<>();

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "area_id", nullable = false)
    private Area area;

    protected Process(){
        super();
    }

    public Process(
        String name,
        String description
    ){
        super(name,description);
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    // Metodos de ayuda para mantener ambos lados sincronizados
    public void addComponent(Component component){
        components.add(component);
        component.setProcess(this);
    }

    public void removeComponent(Component component){
        components.remove(component);
        component.setProcess(null);
    }

    public void deactivate() {
        this.setState(UseState.INACTIVE);
        components.forEach(Component::deactivate);
    }

    public void activate() {
        this.setState(UseState.ACTIVE);
        components.forEach(Component::activate);
    }
}
