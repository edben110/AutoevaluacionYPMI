package com.sem.pmiautoevaluacion.users.service;

import java.util.UUID;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sem.pmiautoevaluacion.selfEvaluation.entity.SelfEvaluation;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationRepository;
import com.sem.pmiautoevaluacion.selfEvaluation.repository.SelfEvaluationPeriodRepository;

import com.sem.pmiautoevaluacion.shared.exception.BadRequestException;
import com.sem.pmiautoevaluacion.shared.exception.ResourceNotFoundException;
import com.sem.pmiautoevaluacion.users.entity.EmailRecord;
import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.repository.EstablishmentRepository;
import com.sem.pmiautoevaluacion.users.repository.UserRepository;

@Service
public class EstablishmentServiceImpl implements EstablishmentService {
    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SelfEvaluationRepository evaluations;
    private final SelfEvaluationPeriodRepository periods;
    private final Clock clock;

    public EstablishmentServiceImpl(
        EstablishmentRepository establishmentRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        SelfEvaluationRepository evaluations,
        SelfEvaluationPeriodRepository periods,
        Clock selfEvaluationClock
    ) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.evaluations = evaluations;
        this.periods = periods;
        this.clock = selfEvaluationClock;
    }

    @Override
    @Transactional
    public Establishment create(
        String name,
        String password,
        String email,
        String daneCode,
        String rector
    ) {
        if(establishmentRepository.existsByDaneCode(daneCode)){
            throw new BadRequestException("Ya existe una institucion con este codigo DANE");
        }
        if(userRepository.existsByEmail_Value(email)){
            throw new BadRequestException("Ya existe un usuario con este correo");
        }

        String encodedPassword = passwordEncoder.encode(password);
        Establishment establishment = new Establishment(
            name,
            encodedPassword,
            new EmailRecord(email),
            daneCode,
            rector
        );

        Establishment saved = establishmentRepository.save(establishment);
        // Una institución registrada durante un período abierto debe poder ingresar a sus cuatro áreas.
        LocalDate today = LocalDate.now(clock);
        periods.findAllByOrderByYearDesc().stream().filter(period -> period.getPeriod().include(today))
            .forEach(period -> {
                SelfEvaluation draft = new SelfEvaluation(saved, period.getYear());
                draft.assignPeriod(period);
                evaluations.save(draft);
            });
        return saved;
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
