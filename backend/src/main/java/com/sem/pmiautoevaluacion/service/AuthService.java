package com.sem.pmiautoevaluacion.service;
import com.sem.pmiautoevaluacion.dto.EstablishmentLoginRequest;
import com.sem.pmiautoevaluacion.dto.LoginResponse;
import com.sem.pmiautoevaluacion.dto.SecretaryLoginRequest;
 
public interface AuthService {
 
    LoginResponse loginSecretary(SecretaryLoginRequest request);
 
    LoginResponse loginEstablishment(EstablishmentLoginRequest request);
}
