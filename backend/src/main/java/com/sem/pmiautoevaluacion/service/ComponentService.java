package com.sem.pmiautoevaluacion.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.entity.Process;
import com.sem.pmiautoevaluacion.entity.Component;
import com.sem.pmiautoevaluacion.repository.ComponentRepository;
import com.sem.pmiautoevaluacion.repository.ProcessRepository;
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

    public Component findByName(String name){
        return componentRepository.findByName(name)
            .map(component -> (Component)component)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el componente con el nombre proporcionado"
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
}
