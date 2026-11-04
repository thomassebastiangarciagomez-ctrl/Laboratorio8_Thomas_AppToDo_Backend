package edu.eci.dosw.todo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import edu.eci.dosw.todo.config.SecurityConfig;
import edu.eci.dosw.todo.controller.AdminController;
import edu.eci.dosw.todo.controller.AuthController;
import edu.eci.dosw.todo.controller.TaskController;
import edu.eci.dosw.todo.controller.UserController;
import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.dto.LoginRequest;
import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.entity.UserRole;
import edu.eci.dosw.todo.exception.EmailAlreadyExistsException;
import edu.eci.dosw.todo.exception.InvalidCredentialsException;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.service.AdminService;
import edu.eci.dosw.todo.service.AuthService;
import edu.eci.dosw.todo.service.TaskService;
import tools.jackson.databind.ObjectMapper;

/**
 * RF-SEG-10: rutas publicas, rutas protegidas, token invalido, acceso cruzado y diferencia entre 401, 403 y 404.
 * Se usa la SecurityConfig real; los servicios y el JwtDecoder se simulan.
 */
@WebMvcTest({ AuthController.class, UserController.class, AdminController.class, TaskController.class })
@Import(SecurityConfig.class)
public class SecurityEndpointsTest {

    private static final String REGISTER_BODY = """
            {"name":"Ana Perez","email":"ana@example.com","password":"ContrasenaDemo123!"}
            """;
    private static final String LOGIN_BODY = """
            {"email":"ana@example.com","password":"ContrasenaDemo123!"}
            """;
    private static final String UPDATE_BODY = """
            {"title":"Intento","description":"x","status":"PENDING","priority":"LOW","dueDate":"2026-12-01"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /** JWT simulado: "sub" = id del usuario y authority ROLE_<rol>, como produce el converter real. */
    private static RequestPostProcessor tokenOf(long userId, String role) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Perfil de Ana construido con el mismo metodo que usa el servicio real. */
    private static UserResponse anaProfile() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("Ana Perez");
        user.setEmail("ana@example.com");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(user);
    }

    // ---------------------------------------------------------------- registro (publico)

    @Test
    public void register_isPublicAndNeverReturnsSecrets() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(anaProfile());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"\",\"email\":\"ana@example.com\",\"password\":\"ContrasenaDemo123!\"}",
            "{\"name\":\"Ana\",\"email\":\"no-es-un-correo\",\"password\":\"ContrasenaDemo123!\"}",
            "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"corta\"}"
    })
    public void register_withInvalidData_returns400(String body) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    public void register_withExistingEmail_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- login (publico)

    @Test
    public void login_withValidCredentials_returns200AndToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(objectMapper.readValue(
                "{\"tokenType\":\"Bearer\",\"accessToken\":\"jwt-de-prueba\",\"expiresIn\":1800}", TokenResponse.class));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    public void login_withInvalidCredentials_returns401WithoutToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void login_withMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ---------------------------------------------------------------- perfil
    
    @Test
    public void me_withToken_returnsOwnProfileWithoutSecrets() throws Exception {
        when(authService.getProfile(1L)).thenReturn(anaProfile());

        mockMvc.perform(get("/api/v1/users/me").with(tokenOf(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---------------------------------------------------------------- tareas protegidas

    @Test
    public void tasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    /** Token alterado o expirado: el decoder lo rechaza y la API responde 401. */
    @Test
    public void tasks_withInvalidToken_returns401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("firma invalida o token expirado"));

        mockMvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    /** El id que usa el servicio sale del "sub" del token, nunca de la URL o del cuerpo. */
    @Test
    public void tasks_useUserIdFromToken() throws Exception {
        when(taskService.findAll(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tasks").with(tokenOf(7L, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(taskService).findAll(7L);
    }

    /** Acceso cruzado: la tarea 25 es de Luis (id 1). Ana (id 2) recibe 404 en consulta, edicion y eliminacion. */
    @Test
    public void tasks_ofAnotherUser_return404() throws Exception {
        when(taskService.findById(25L, 2L)).thenThrow(new TaskNotFoundException(25L));
        when(taskService.update(eq(25L), any(TaskUpdateRequest.class), eq(2L)))
                .thenThrow(new TaskNotFoundException(25L));
        doThrow(new TaskNotFoundException(25L)).when(taskService).delete(25L, 2L);

        mockMvc.perform(get("/api/v1/tasks/25").with(tokenOf(2L, "USER"))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/tasks/25").with(tokenOf(2L, "USER"))
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/tasks/25").with(tokenOf(2L, "USER"))).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- recurso administrativo

    @Test
    public void adminSummary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary")).andExpect(status().isUnauthorized());
    }

    /** Token valido pero con rol USER: autenticado, sin privilegios -> 403 (no 401). */
    @Test
    public void adminSummary_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/summary").with(tokenOf(1L, "USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    public void adminSummary_withAdminRole_returns200WithAggregates() throws Exception {
        when(adminService.getSummary()).thenReturn(objectMapper.readValue(
                "{\"totalUsers\":3,\"totalTasks\":12,\"tasksByStatus\":{\"PENDING\":7,\"IN_PROGRESS\":3,\"COMPLETED\":2}}",
                AdminSummaryResponse.class));

        mockMvc.perform(get("/api/v1/admin/summary").with(tokenOf(9L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.totalTasks").value(12))
                .andExpect(jsonPath("$.tasksByStatus.PENDING").value(7));
    }
}
