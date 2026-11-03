package edu.eci.dosw.todo.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.todo.dto.LoginRequest;
import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.entity.UserRole;
import edu.eci.dosw.todo.exception.EmailAlreadyExistsException;
import edu.eci.dosw.todo.exception.InvalidCredentialsException;
import edu.eci.dosw.todo.repository.UserRepository;
import edu.eci.dosw.todo.security.JwtService;

@Service 
public class AuthServiceImpl implements AuthService{
    
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    /**
     * Este metodo se encarga de la logica dentras de la implementacion del registro de un nuevo usuario
     * en la aplicacion web
     */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request){
        String email = request.getEmail().trim().toLowerCase();

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException();
        }

        UserEntity entity = new UserEntity();
        entity.setName(request.getName());
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        entity.setRole(UserRole.USER);
        entity.setCreatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(entity);
        return UserResponse.fromEntity(saved);
    }

    /**
     * Este metodo se encarga de la logica detras del registro de un usuario que ya cuenta con una cuenta
     * dentro de la aplicacion web
     */
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request){
        UserEntity user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                        .filter(entity -> passwordEncoder.matches(request.getPassword(), entity.getPasswordHash()))
                        .orElseThrow(() -> new InvalidCredentialsException());
        return jwtService.generateToken(user);
    }

    /**
     * Este metodo se encarga de realizar la logica detras de la consulta de perfil por parte de los usuarios
     * que hayan ingresado a su cuenta.
     */
    @Override
    @Transactional
    public UserResponse getProfile(Long id){
        return userRepository.findById(id)
                .map(entity -> UserResponse.fromEntity(entity))
                .orElseThrow(() -> new InvalidCredentialsException());
    }
}
