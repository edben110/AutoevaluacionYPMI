package com.sem.pmiautoevaluacion.integralManagement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.integralManagement.entity.Component;
import com.sem.pmiautoevaluacion.integralManagement.entity.Process;
import com.sem.pmiautoevaluacion.integralManagement.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.ProcessRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;

@Service 
public class ComponentService {
    private final ComponentRepository componentRepository;
    private final ProcessRepository processRepository;

    public ComponentService(
        ComponentRepository componentRepository,
        ProcessRepository processRepository
    ) {
        this.componentRepository = componentRepository;
        this.processRepository = processRepository;
    }

    public Component create(
        UUID processId,
        String name,
        String description
    ) {
        if(componentRepository.existsByName(name)){
            throw new BadRequestException(
                "Ya existe un componente con ese nombre"
            );
        }

        Process process = processRepository.findById(processId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el Id proporcionado"
            ));

        if(process.getState() != UseState.ACTIVE) {
            throw new BadRequestException(
                "El proceso no se encuentra activo para su uso o registro"
            );
        }
        
        Component component = new Component(name, description);
        // Mantiene sincronizados ambos finales
        process.addComponent(component);
        
        return componentRepository.save(component);
    }

    public Component findById(UUID id){
        return componentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el componente con el Id proporcionado"
            ));
    }

    public Component findActiveById(UUID id){
        return componentRepository.findByIdAndState(id, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un componente activo con el Id proporcionado"
            ));
    }

    public Component findByName(String name){
        return componentRepository.findByName(name)
            .map(component -> (Component)component)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el componente con el nombre proporcionado"
            ));
    }

    public Component findActiveByName(String name) {
        return componentRepository.findByNameAndState(name, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un componente activo con el nombre proporcionado"
            ));
    }

    public List<Component> findByProcessId(UUID processId){
        if(!processRepository.existsById(processId)){
            throw new ResourceNotFoundException(
                "No se encontro un proceso con el Id proporcionado"
            );
        }

        return componentRepository.findByProcessId(processId);
    }

    public List<Component> findActiveByProcessId(UUID processId){
        if(!processRepository.existsByIdAndState(processId, UseState.ACTIVE)){
            throw new ResourceNotFoundException(
                "No se encontro un proceso activo con el Id proporcionado"  
            );
        }

        return componentRepository.findByProcessIdAndState(processId, UseState.ACTIVE);
    }

    public List<Component> findAll() {
        return componentRepository.findAll();
    }

    public List<Component> findAllActive() {
        return componentRepository.findByState(UseState.ACTIVE);
    }
}
