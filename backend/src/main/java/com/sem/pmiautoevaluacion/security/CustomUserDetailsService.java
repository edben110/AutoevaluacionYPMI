package com.sem.pmiautoevaluacion.security;

import com.sem.pmiautoevaluacion.entity.User;
import com.sem.pmiautoevaluacion.repository.UserRepository;
import com.sem.pmiautoevaluacion.repository.EstablishmentRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;

    public CustomUserDetailsService(
        UserRepository userRepository,
        EstablishmentRepository establishmentRepository
    ) {
        this.userRepository = userRepository;
        this.establishmentRepository = establishmentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. intenta como daneCode (Establishment)
        var establishment = establishmentRepository.findByDaneCode(identifier);
        if (establishment.isPresent()) {
            return new CustomUserDetails(establishment.get());
        }

        // 2. Si no fue por daneCode, intenta como Email (Secretary)
        User user = userRepository.findByEmail_Value(identifier)
            .orElseThrow(()-> new UsernameNotFoundException(
                "No se encontro un usuario con el identificador proporcionado"));
        
        return new CustomUserDetails(user);
    }

}
