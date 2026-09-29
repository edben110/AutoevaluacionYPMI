package com.sem.pmiautoevaluacion.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.entity.Area;
import com.sem.pmiautoevaluacion.entity.Process;
import com.sem.pmiautoevaluacion.repository.AreaRepository;
import com.sem.pmiautoevaluacion.repository.ProcessRepository;
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

    public Process findByName(String name){
        return processRepository.findByName(name)
            .map(process -> (Process)process)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el nombre espeficado"
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
}
