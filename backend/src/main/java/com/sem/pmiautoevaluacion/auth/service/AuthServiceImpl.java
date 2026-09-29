package com.sem.pmiautoevaluacion.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.sem.pmiautoevaluacion.auth.dto.EstablishmentLoginRequest;
import com.sem.pmiautoevaluacion.auth.dto.LoginResponse;
import com.sem.pmiautoevaluacion.auth.dto.SecretaryLoginRequest;
import com.sem.pmiautoevaluacion.auth.security.CustomUserDetails;
import com.sem.pmiautoevaluacion.auth.security.JwtService;
import com.sem.pmiautoevaluacion.entity.Establishment;
import com.sem.pmiautoevaluacion.entity.Secretary;
import com.sem.pmiautoevaluacion.entity.User;

@Service 
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    };

    // Solo secretary puede entrar por este endpoint
    @Override 
    public LoginResponse loginSecretary(SecretaryLoginRequest request) {
        CustomUserDetails userDetails = authenticate(request.getEmail(), request.getPassword());

        if (!(userDetails.getUser() instanceof Secretary)) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        return buildResponse(userDetails, "SECRETARY");
    }

    // Solo establishment puede entrar por este endpoint
    @Override 
    public LoginResponse loginEstablishment(EstablishmentLoginRequest request){
        CustomUserDetails userDetails = authenticate(request.getDaneCode(), request.getPassword());

        if(!(userDetails.getUser() instanceof Establishment)) {
            throw new BadCredentialsException("Credenciales Incorrectas");
        }

        return buildResponse(userDetails, "ESTABLISHMENT");
    }

    // Delegaciones a Spring security
    private CustomUserDetails authenticate(String identifier, String password) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(identifier, password)
        );
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private LoginResponse buildResponse(CustomUserDetails userDetails, String role) {
        User user = userDetails.getUser();
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(token, user.getId(), user.getName(), role);
    }

}
