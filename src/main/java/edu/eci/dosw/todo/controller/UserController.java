package edu.eci.dosw.todo.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.service.AuthService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private AuthService authService;

    public UserController(AuthService authService){
        this.authService = authService;
    }

    /**
     * 
     * @param jwt
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(authService.getProfile(Long.valueOf(jwt.getSubject())));
    }
}
