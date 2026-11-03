package edu.eci.dosw.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;
import edu.eci.dosw.todo.service.TaskServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TaskServicesTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskEntity existingTask;

    @BeforeEach
    public void setUp() {
        existingTask = new TaskEntity();
        existingTask.setId(1L);
        existingTask.setTitle("Estudiar para el parcial");
        existingTask.setDescription("Repasar JPA");
        existingTask.setStatus(TaskStatus.PENDING);
        existingTask.setPriority(TaskPriority.HIGH);
        existingTask.setDueDate(LocalDate.of(2026, 10, 1));
        existingTask.setCreatedAt(LocalDateTime.now());
    }

    // ---------------------------------------------------------------- findAll

    /**
     * Solo se consultan las tareas del usuario autenticado. Se verifica ademas que nunca se use findAll() del
     * repositorio, que devolveria las tareas de todos los usuarios.
     */
    @Test
    public void findAll_shouldReturnOnlyTasksOfUser() {
        when(taskRepository.findByUserId(USER_ID)).thenReturn(List.of(existingTask));

        List<TaskResponse> result = taskService.findAll(USER_ID);

        assertEquals(1, result.size());
        verify(taskRepository, never()).findAll();
    }

    // ---------------------------------------------------------------- findById

    @Test
    public void findById_shouldReturnTaskWhenExists() {
        when(taskRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        TaskResponse task = taskService.findById(1L, USER_ID);

        assertEquals(1L, task.getId());
    }

    @Test
    public void findById_shouldThrowExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.findById(99L, USER_ID));
    }

    /**
     * Acceso cruzado: la tarea 1 existe pero pertenece a otro usuario. Para el servicio es indistinguible de una
     * tarea inexistente (404), y nunca se busca solo por id.
     */
    @Test
    public void findById_shouldThrowExceptionWhenTaskBelongsToAnotherUser() {
        when(taskRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.findById(1L, OTHER_USER_ID));
        verify(taskRepository, never()).findById(any());
    }

    // ---------------------------------------------------------------- create

    /** La tarea creada queda asociada al usuario autenticado. */
    @Test
    public void create_shouldCreateTaskForAuthenticatedUser() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Nueva tarea");
        request.setDescription("Descripción de prueba");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.of(2026, 11, 1));

        UserEntity owner = new UserEntity();
        owner.setId(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        TaskResponse result = taskService.create(request, USER_ID);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());
        assertSame(owner, captor.getValue().getUser());
        assertEquals(10L, result.getId());
        assertEquals("Nueva tarea", result.getTitle());
    }

    /** Valores por defecto cuando el request no los trae: estado PENDING y prioridad MEDIUM. */
    @Test
    public void create_shouldAssignDefaults() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Nueva tarea");
        request.setDescription("Descripción de prueba");
        request.setDueDate(LocalDate.of(2026, 11, 1));

        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });

        TaskResponse result = taskService.create(request, USER_ID);

        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        // LocalDateTime.now() nunca coincide exactamente entre dos llamadas: se comprueba que exista y no sea futura.
        assertNotNull(result.getCreatedAt());
        assertFalse(result.getCreatedAt().isAfter(LocalDateTime.now()));
    }

    // ---------------------------------------------------------------- update

    @Test
    public void update_shouldUpdateExistingTask() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Título actualizado");
        request.setDescription("Nueva descripcion");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.LOW);
        request.setDueDate(LocalDate.of(2026, 12, 1));

        when(taskRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.update(1L, request, USER_ID);

        assertEquals("Nueva descripcion", result.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    public void update_shouldThrowExceptionWhenTaskDoesNotExist() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Título actualizado");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.LOW);

        when(taskRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.update(99L, request, USER_ID));
    }

    /** Un usuario no puede modificar la tarea de otro: no se llega a guardar nada. */
    @Test
    public void update_shouldNotModifyTaskOfAnotherUser() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Intento de modificacion");
        request.setStatus(TaskStatus.COMPLETED);
        request.setPriority(TaskPriority.LOW);

        when(taskRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.update(1L, request, OTHER_USER_ID));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    // ---------------------------------------------------------------- delete

    @Test
    public void delete_shouldDeleteExistingTask() {
        when(taskRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingTask));

        taskService.delete(1L, USER_ID);

        verify(taskRepository, times(1)).delete(existingTask);
    }

    @Test
    public void delete_shouldThrowExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.delete(99L, USER_ID));
    }

    /** Un usuario no puede eliminar la tarea de otro: delete nunca se ejecuta. */
    @Test
    public void delete_shouldNotDeleteTaskOfAnotherUser() {
        when(taskRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.delete(1L, OTHER_USER_ID));
        verify(taskRepository, never()).delete(any(TaskEntity.class));
    }
}