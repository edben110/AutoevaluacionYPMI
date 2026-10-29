package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;

@Service 
public class SecretaryServiceImpl implements SecretaryService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecretaryServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override 
    public Secretary create(
        String name,
        String password,
        String email
    ) {
        if(userRepository.existsByEmail_Value(email)){
            throw new BadRequestException("Ya existe un usuario con este correo");
        }

        String encodedPassword = passwordEncoder.encode(password);

        Secretary secretary = new Secretary(
            name,
            encodedPassword,
            new EmailRecord(email)
        );
        
        return userRepository.save(secretary);
    }

    @Override 
    public Secretary findById(UUID id){
        return userRepository.findById(id)
            .map(user -> {
                if(user instanceof Secretary secretary) {
                    return secretary;
                }

                throw new ResourceNotFoundException(
                    "El usuario no es de secretaria"
                );
            })
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el usuario con el Id proporcionado"
            ));
    }

    @Override
    public Secretary findByEmail(String email){
        return userRepository.findByEmail_Value(email)
            .filter(user -> user instanceof Secretary)
            .map(user -> (Secretary) user)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro un usuario con el correo proporcionado"
            ));
    }
}
