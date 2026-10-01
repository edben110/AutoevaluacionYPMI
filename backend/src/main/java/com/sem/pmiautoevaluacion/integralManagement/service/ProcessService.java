package com.sem.pmiautoevaluacion.integralManagement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.integralManagement.entity.Area;
import com.sem.pmiautoevaluacion.integralManagement.entity.Process;
import com.sem.pmiautoevaluacion.integralManagement.repository.AreaRepository;
import com.sem.pmiautoevaluacion.integralManagement.repository.ProcessRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;

@Service 
public class ProcessService {
    private final ProcessRepository processRepository;
    private final AreaRepository areaRepository;

    public ProcessService(
        ProcessRepository processRepository,
        AreaRepository areaRepository
    ) {
        this.processRepository = processRepository;
        this.areaRepository = areaRepository;
    }

    public Process create(
        UUID areaId,
        String name,
        String description
    ) {
        if(processRepository.existsByName(name)){
            throw new BadRequestException(
                "Ya existe un proceso con ese nombre"
            );
        }

        Area area = areaRepository.findById(areaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el area con el Id proporcionado"
            ));

        if (area.getState() != UseState.ACTIVE) {
            throw new BadRequestException(
                "El area no se encuentra activa para uso o registro"
            );
        }
        
        Process process = new Process(name, description);
        // Mantiene sincronizados ambos finales
        area.addProcess(process);

        return processRepository.save(process);
    }

    public Process findById(UUID id){
        return processRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el Id Proporcionado"
            ));     
    }

    public Process findActiveById(UUID id) {
        return processRepository.findByIdAndState(id, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un proceso activo con el Id Proporcionado"
            ));
    }

    public Process findByName(String name){
        return processRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el nombre espeficado"
            ));
    }

    public Process findActiveByName(String name){
        return processRepository.findByNameAndState(name, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un proceso activo con el nombre proporcionado"
            ));
    }

    public List<Process> findByAreaId(UUID areaId) {
        if(!areaRepository.existsById(areaId)){
            throw new ResourceNotFoundException(
                "No se encontro un area con el Id proporcionado"
            );
        } 
        return processRepository.findByAreaId(areaId);
    }

    public List<Process> findActiveByAreaId(UUID areaId) {
        if(!areaRepository.existsByIdAndState(areaId, UseState.ACTIVE)){
            throw new ResourceNotFoundException(
                "No se encontro un area activa con el Id Proporcionado"
            );
        }

        return processRepository.findByAreaIdAndState(
            areaId,
            UseState.ACTIVE
        );
    }

    public List<Process> findAll() {
        return processRepository.findAll();
    }

    public List<Process> findAllActive() {
        return processRepository.findByState(UseState.ACTIVE);
    }
}
