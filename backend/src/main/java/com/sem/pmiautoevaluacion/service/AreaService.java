package com.sem.pmiautoevaluacion.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.entity.Area;
import com.sem.pmiautoevaluacion.repository.AreaRepository;
import com.sem.pmiautoevaluacion.shared.enums.UseState;
import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;

@Service 
public class AreaService {
    private final AreaRepository areaRepository;

    public AreaService(AreaRepository areaRepository){
        this.areaRepository = areaRepository;
    }

    public Area create(
        String name,
        String description
    ){
        if(areaRepository.existsByName(name)){
            throw new BadRequestException(
                "Ya existe un area con este nombre"
            );
        }

        Area area = new Area(name,description);
        return areaRepository.save(area);
    }

    public Area findById(UUID id){
        return areaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el area con el Id proporcionado"
            ));
    }

    public Area findActiveById(UUID id) {
        return areaRepository.findByIdAndState(id, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un area activa con el Id proporcionado"
            ));
    }

    public Area findByName(String name){
        return areaRepository.findByName(name)
            .orElseThrow(()-> new ResourceNotFoundException(
                "No se encontro el area con el nombre especificado"
            ));
    }

    public Area findActiveByName(String name){
        return areaRepository.findByNameAndState(name, UseState.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un area activa con el nombre proporcionado"
            ));
    }

    public List<Area> findAll() {
        return areaRepository.findAll();
    }

    public List<Area> findAllActive() {
        return areaRepository.findByState(UseState.ACTIVE);
    }
}
