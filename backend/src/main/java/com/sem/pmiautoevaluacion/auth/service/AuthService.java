package com.sem.pmiautoevaluacion.auth.service;
import com.sem.pmiautoevaluacion.auth.dto.EstablishmentLoginRequest;
import com.sem.pmiautoevaluacion.auth.dto.LoginResponse;
import com.sem.pmiautoevaluacion.auth.dto.SecretaryLoginRequest;
 
public interface AuthService {
 
    LoginResponse loginSecretary(SecretaryLoginRequest request);
 
    LoginResponse loginEstablishment(EstablishmentLoginRequest request);
}
