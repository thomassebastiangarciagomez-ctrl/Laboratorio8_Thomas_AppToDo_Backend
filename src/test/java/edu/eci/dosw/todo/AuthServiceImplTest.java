package edu.eci.dosw.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import edu.eci.dosw.todo.service.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    // Los DTOs de entrada se simulan: asi la prueba solo depende de sus getters y no de su constructor o setters.
    private RegisterRequest registerRequest(String name, String email, String password) {
        RegisterRequest request = mock(RegisterRequest.class);
        lenient().when(request.getName()).thenReturn(name);
        lenient().when(request.getEmail()).thenReturn(email);
        lenient().when(request.getPassword()).thenReturn(password);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = mock(LoginRequest.class);
        lenient().when(request.getEmail()).thenReturn(email);
        lenient().when(request.getPassword()).thenReturn(password);
        return request;
    }

    private UserEntity storedUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("Ana Perez");
        user.setEmail("ana@example.com");
        user.setPasswordHash("hash-bcrypt");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    // ---------------------------------------------------------------- registro

    /** Reglas 1, 3 y 4: correo normalizado, contraseña con hash y rol siempre USER. */
    @Test
    public void register_shouldCreateUserWithRoleUserAndHashedPassword() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ContrasenaDemo123!")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = authService
                .register(registerRequest("Ana Perez", " ANA@Example.com ", "ContrasenaDemo123!"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("ana@example.com", saved.getEmail());
        assertEquals("Ana Perez", saved.getName());
        assertEquals(UserRole.USER, saved.getRole());
        assertEquals("hash-bcrypt", saved.getPasswordHash());
        assertNotEquals("ContrasenaDemo123!", saved.getPasswordHash());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(response);
    }

    @Test
    public void register_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest("Ana", "Ana@Example.com", "ContrasenaDemo123!")));

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------------------------------------------------------- login

    @Test
    public void login_shouldReturnTokenWhenCredentialsAreValid() {
        UserEntity user = storedUser();
        TokenResponse token = mock(TokenResponse.class);
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ContrasenaDemo123!", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(token);

        TokenResponse result = authService.login(loginRequest(" ANA@example.com ", "ContrasenaDemo123!"));

        assertSame(token, result);
    }

    @Test
    public void login_shouldRejectWrongPasswordWithoutIssuingToken() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(storedUser()));
        when(passwordEncoder.matches("incorrecta", "hash-bcrypt")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("ana@example.com", "incorrecta")));

        verifyNoInteractions(jwtService);
    }

    /** Correo desconocido y contraseña incorrecta producen el mismo error: no se revela si el correo existe. */
    @Test
    public void login_shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("nadie@example.com", "cualquiera")));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    // ---------------------------------------------------------------- perfil

    @Test
    public void getProfile_shouldReturnProfileOfExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser()));

        UserResponse response = authService.getProfile(1L);

        assertNotNull(response);
        verify(userRepository).findById(1L);
    }

    @Test
    public void getProfile_shouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.getProfile(99L));
    }
}
