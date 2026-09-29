package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;

public class EstablishmentServiceImpl implements EstablishmentService {
    private final EstablishmentRepository establishmentRepository;
    private final PasswordEncoder passwordEncoder;

    public EstablishmentServiceImpl(
        EstablishmentRepository establishmentRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.establishmentRepository = establishmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override 
    public Establishment create(
        String rector,
        String daneCode,
        String password,
        // TODO: Eliminar si se remueve el email de establishment
        String email,
        String name
    ) {
        if(establishmentRepository.existsByDaneCode(daneCode)){
            throw new BadRequestException("Ya existe una institucion con este codigo DANE");
        }

        String encodedPassword = passwordEncoder.encode(password);
        // TODO: Eliminar name e EmailRecord si se remueven los emails de Establishment
        Establishment establishment = new Establishment(
            name,
            encodedPassword,
            new EmailRecord(email),
            daneCode,
            rector
        );

        return establishmentRepository.save(establishment);
    }

    @Override
    public Establishment findById(UUID id) {
        return establishmentRepository.findById(id)
            .map(user -> {
                if (user instanceof Establishment establishment) {
                    return establishment;
                }

                throw new ResourceNotFoundException(
                    "El usuario no es una institucion"
                );
            })
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro al usuario con el ID proporcionado"
            ));
    }

    @Override 
    public Establishment findByDaneCode(String daneCode){
        return establishmentRepository.findByDaneCode(daneCode)
            .filter(user -> user instanceof Establishment)
            .map(user -> (Establishment) user)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el usuario con el codigo DANE proporcionado"
            ));
    }
}
