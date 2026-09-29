package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.entity.User;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;

@Service 
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository){
        this.userRepository = userRepository;
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
    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "No se encontro el usuario con el ID proporcionado"
            );
        }

        userRepository.deleteById(id);
    }
}
