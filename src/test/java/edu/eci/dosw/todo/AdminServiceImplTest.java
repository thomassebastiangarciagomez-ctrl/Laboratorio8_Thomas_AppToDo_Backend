package edu.eci.dosw.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;
import edu.eci.dosw.todo.service.AdminServiceImpl;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {

    // El resumen se lee como JSON (lo mismo que recibe el cliente), asi la prueba no depende de si el DTO es
    // un record o una clase con getters.
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    /** El resumen solo contiene conteos agregados: nada de tareas ni datos de usuarios. */
    @Test
    public void getSummary_shouldReturnAggregatedMetrics() {
        when(userRepository.count()).thenReturn(3L);
        when(taskRepository.count()).thenReturn(12L);
        when(taskRepository.countByStatus(TaskStatus.PENDING)).thenReturn(7L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(3L);
        when(taskRepository.countByStatus(TaskStatus.COMPLETED)).thenReturn(2L);

        JsonNode summary = MAPPER.valueToTree(adminService.getSummary());

        assertEquals(3L, summary.get("totalUsers").longValue());
        assertEquals(12L, summary.get("totalTasks").longValue());
        assertEquals(7L, summary.get("tasksByStatus").get("PENDING").longValue());
        assertEquals(3L, summary.get("tasksByStatus").get("IN_PROGRESS").longValue());
        assertEquals(2L, summary.get("tasksByStatus").get("COMPLETED").longValue());
    }

    /** Todos los estados aparecen, incluso los que tienen 0 tareas. */
    @Test
    public void getSummary_shouldListEveryStatus() {
        JsonNode summary = MAPPER.valueToTree(adminService.getSummary());

        for (TaskStatus status : TaskStatus.values()) {
            assertTrue(summary.get("tasksByStatus").has(status.name()));
        }
        assertEquals(0L, summary.get("totalUsers").longValue());
    }
}
