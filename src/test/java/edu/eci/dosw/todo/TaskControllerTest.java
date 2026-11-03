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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import edu.eci.dosw.todo.controller.TaskController;
import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.service.TaskService;
import tools.jackson.databind.ObjectMapper;

/**
 * Pruebas de la capa web de tareas. Ahora el controlador esta protegido, por eso se importa la SecurityConfig real
 * y cada peticion lleva un JWT simulado cuyo "sub" es el id del usuario autenticado.
 */
@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
public class TaskControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    // Requerido por oauth2ResourceServer de la SecurityConfig; no se usa cuando se envia jwt() simulado.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private TaskResponse sampleResponse;

    @BeforeEach
    public void setUp() {
        TaskEntity entity = new TaskEntity();
        entity.setId(1L);
        entity.setTitle("Terminar laboratorio DOSW");
        entity.setDescription("Completar pruebas del backend");
        entity.setStatus(TaskStatus.PENDING);
        entity.setPriority(TaskPriority.HIGH);
        entity.setDueDate(LocalDate.of(2026, 9, 25));
        entity.setCreatedAt(LocalDateTime.now());

        sampleResponse = TaskResponse.fromEntity(entity);
    }

    /** JWT simulado de un usuario con rol USER; el backend toma el id del claim "sub". */
    private RequestPostProcessor authenticated() {
        return jwt().jwt(token -> token.subject(String.valueOf(USER_ID)).claim("role", "USER"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private TaskCreateRequest validCreateRequest() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Terminar laboratorio DOSW");
        request.setDescription("Completar pruebas del backend");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.of(2026, 9, 25));
        return request;
    }

    private TaskUpdateRequest validUpdateRequest() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Terminar laboratorio DOSW");
        request.setDescription("Completar pruebas del backend");
        request.setPriority(TaskPriority.HIGH);
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setDueDate(LocalDate.of(2026, 9, 25));
        return request;
    }

    // ---------------------------------------------------------------- GET

    /** Consulta de las tareas propias: se espera 200 y que el servicio reciba el id del token. */
    @Test
    public void should_GetOk() throws Exception {
        when(taskService.findAll(USER_ID)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/tasks").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskService).findAll(USER_ID);
    }

    /** Consulta valida de una tarea propia: se espera 200. */
    @Test
    public void should_findByIdOk() throws Exception {
        when(taskService.findById(1L, USER_ID)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/tasks/1").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /** Tarea inexistente (o ajena, para el servicio es lo mismo): se espera 404. */
    @Test
    public void should_not_findById() throws Exception {
        when(taskService.findById(99L, USER_ID)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/v1/tasks/99").with(authenticated()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- POST

    /** Creacion valida: se espera 201 y que el dueño sea el usuario del token. */
    @Test
    public void should_postTask() throws Exception {
        when(taskService.create(any(TaskCreateRequest.class), eq(USER_ID))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/tasks").with(authenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).create(any(TaskCreateRequest.class), eq(USER_ID));
    }

    /** Datos invalidos (sin titulo): se espera 400. */
    @Test
    public void should_not_postTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setPriority(TaskPriority.HIGH);

        mockMvc.perform(post("/api/v1/tasks").with(authenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(taskService);
    }

    // ---------------------------------------------------------------- PUT

    /** Actualizacion valida de una tarea propia: se espera 200. */
    @Test
    public void should_putOk() throws Exception {
        when(taskService.update(eq(1L), any(TaskUpdateRequest.class), eq(USER_ID))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/tasks/1").with(authenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    /** Actualizar una tarea inexistente o ajena: se espera 404. */
    @Test
    public void should_not_put() throws Exception {
        when(taskService.update(eq(99L), any(TaskUpdateRequest.class), eq(USER_ID)))
                .thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/v1/tasks/99").with(authenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- DELETE

    /** Eliminacion de una tarea propia: se espera 204. */
    @Test
    public void should_deleteTask() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/1").with(authenticated()))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L, USER_ID);
    }

    /** Eliminar una tarea inexistente o ajena: se espera 404. */
    @Test
    public void should_not_deleteTask() throws Exception {
        doThrow(new TaskNotFoundException(99L)).when(taskService).delete(99L, USER_ID);

        mockMvc.perform(delete("/api/v1/tasks/99").with(authenticated()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- SEGURIDAD

    /** Sin cabecera Authorization ninguna operacion del CRUD debe ejecutarse: se espera 401. */
    @Test
    public void should_rejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/tasks/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/tasks/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/tasks/1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    /** Un token que el decoder rechaza (alterado o expirado) tambien produce 401. */
    @Test
    public void should_rejectInvalidToken() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("token invalido"));

        mockMvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer token.alterado.xyz"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }
}