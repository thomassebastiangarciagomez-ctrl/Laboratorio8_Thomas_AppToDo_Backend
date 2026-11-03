package edu.eci.dosw.todo.service;
import edu.eci.dosw.todo.dto.*;
import java.util.*;

public interface TaskService {
    
    List<TaskResponse> findAll(Long userId);

    TaskResponse findById(Long Id, Long userId);

    TaskResponse create(TaskCreateRequest request, Long userId);

    TaskResponse update(Long id, TaskUpdateRequest request, Long userId);

    void delete(Long id, Long userId);
}
