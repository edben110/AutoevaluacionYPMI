package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.User;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;

@Service 
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
        UserRepository userRepository,
        EstablishmentRepository establishmentRepository,
        PasswordEncoder passwordEncoder
    ){
        this.userRepository = userRepository;
        this.establishmentRepository = establishmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el usuario con el ID proporcionado"
            ));
    }

    @Override 
    public User findByEmail(String email) {
        return userRepository.findByEmail_Value(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el usuario con el Correo proporcionado"
            ));
    }

    @Override 
    public boolean existsByEmail(String email){
        return userRepository.existsByEmail_Value(email);
    }

    @Override
    @Transactional
    public User update(
        User user,
        String name,
        String email,
        String password,
        String daneCode,
        String rector
    ) {
        if (name != null) {
            user.setName(name);
        }

        if (email != null) {
            userRepository.findByEmail_Value(email)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new BadRequestException("Ya existe un usuario con este correo");
                });
            user.setEmail(new EmailRecord(email));
        }

        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (user instanceof Establishment establishment) {
            if (daneCode != null) {
                establishmentRepository.findByDaneCode(daneCode)
                    .filter(existingEstablishment ->
                        !existingEstablishment.getId().equals(establishment.getId()))
                    .ifPresent(existingEstablishment -> {
                        throw new BadRequestException(
                            "Ya existe una institucion con este codigo DANE"
                        );
                    });
                establishment.setDaneCode(daneCode);
            }
            if (rector != null) {
                establishment.setRector(rector);
            }
        } else if (daneCode != null || rector != null) {
            throw new BadRequestException(
                "Los campos daneCode y rector solo aplican para establecimientos"
            );
        }

        return userRepository.save(user);
    }

    @Override 
    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "No se encontro el usuario con el ID proporcionado"
            );
        }

        userRepository.deleteById(id);
    }
}
