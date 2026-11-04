package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.LoginRequest;
import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.dto.UserResponse;

public interface AuthService {
    
    UserResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    UserResponse getProfile(Long id);
}
