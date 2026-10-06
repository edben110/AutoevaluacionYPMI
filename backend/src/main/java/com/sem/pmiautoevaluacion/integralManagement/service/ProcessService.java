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

import jakarta.transaction.Transactional;

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

    // Creacion
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

    // Actualizacion
    @Transactional 
    public Process update(
        UUID id,
        String name,
        String description
    ) {
        Process process = findById(id);

        if(!process.getName().equalsIgnoreCase(name)
            && processRepository.existsByName(name)) {
                throw new BadRequestException(
                    "Ya existe un proceso con ese nombre"
                );
        }

        process.setName(name);
        process.setDescription(description);

        return process;
    }

    // Desactivar un proceso -> Resulta en desactivacion en cascada
    @Transactional 
    public Process deactivate(UUID id) {
        Process process = findById(id);

        if (process.getState() == UseState.INACTIVE) {
            throw new BadRequestException(
                "El proceso ya se encuentra inactivo"
            );
        }

        process.deactivate();
        return process;
    }

    // Activar un proceso
    @Transactional 
    public Process activate(UUID id) {
        Process process = findById(id);

        if(process.getState() == UseState.ACTIVE) {
            throw new BadRequestException(
                "El proceso ya se encuentra activo"
            );
        }

        if(process.getArea().getState() == UseState.INACTIVE) {
            throw new BadRequestException(
                "No se puede activar un proceso cuyo area se encuentra inactiva"
            );
        }

        process.activate();
        return process;
    }

    // Cambiar el area padre de un proceso
    @Transactional 
    public Process changeArea(UUID id,UUID areaId) {
        Process process = findById(id);

        Area oldArea = process.getArea();
        if (oldArea.getId().equals(areaId)) {
            throw new BadRequestException("El proceso ya pertenece a esa area");
        }

        Area newArea = areaRepository.findById(areaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un area con el Id proporcionado"
            ));

        if(newArea.getState() != UseState.ACTIVE) {
            throw new BadRequestException(
                "El area debe estar Activa para poder usarla"
            );
        }

        oldArea.removeProcess(process);
        newArea.addProcess(process);
        return process;
    }

    // Encontrar por Id
    public Process findById(UUID id){
        return processRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el Id Proporcionado"
            ));     
    }

    // Encontrar por Id, filtrando por estado Activo
    public Process findActiveById(UUID id) {
        return processRepository.findByIdAndState(id, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un proceso activo con el Id Proporcionado"
            ));
    }

    // Encontrar por Nombre
    public Process findByName(String name){
        return processRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el proceso con el nombre espeficado"
            ));
    }

    // Encontrar por Nombre, filtrando por estado Activo
    public Process findActiveByName(String name){
        return processRepository.findByNameAndState(name, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un proceso activo con el nombre proporcionado"
            ));
    }

    // Encontrar usando el Id de un Area
    public List<Process> findByAreaId(UUID areaId) {
        if(!areaRepository.existsById(areaId)){
            throw new ResourceNotFoundException(
                "No se encontro un area con el Id proporcionado"
            );
        } 
        return processRepository.findByAreaId(areaId);
    }

    // Encontrar usando el Id de un Area, filtrando por Activo
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

    // Encontrar todos
    public List<Process> findAll() {
        return processRepository.findAll();
    }

    // Encontrar todos los procesos con estado Activo
    public List<Process> findAllActive() {
        return processRepository.findByState(UseState.ACTIVE);
    }
}
