package edu.eci.dosw.todo.security;

import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.entity.UserEntity;

public interface JwtService {
    
    TokenResponse generateToken(UserEntity entity);

}
