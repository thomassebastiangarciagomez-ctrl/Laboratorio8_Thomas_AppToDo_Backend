package edu.eci.dosw.todo.service;
import java.time.LocalDateTime;
import java.util.*;
import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;

import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService{
    
    private TaskRepository taskRepository;
    private UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository){
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * 
     */
    @Override
    public List<TaskResponse> findAll(Long userId){
        return taskRepository.findByUserId(userId).stream()
                .map(entity -> TaskResponse.fromEntity(entity))
                .toList();
    }

    /**
     * 
     */
    @Override
    public TaskResponse findById(Long id, Long userId){
        TaskEntity entity = taskRepository.findByIdAndUserId(id, userId)
                 .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponse.fromEntity(entity);
    }

    /**
     * Este metodo se encarga de realizar la logica detras de crear una nueva tarea dentro de la
     * aplicacion web
     */
    @Override
    public TaskResponse create(TaskCreateRequest request, Long userId){
        TaskEntity entity = new TaskEntity();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStatus(TaskStatus.PENDING);
        entity.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        entity.setDueDate(request.getDueDate());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUser(userRepository.getReferenceById(userId));

        TaskEntity saved = taskRepository.save(entity);
        return TaskResponse.fromEntity(saved);
    }

    /**
     * Este metodo se encarga de realizar la actualizacion de los datos de una tarea en especifico dentro
     * de la aplicacion web
     */
    @Override
    public TaskResponse update(Long id, TaskUpdateRequest request, Long userId){
        TaskEntity entity = taskRepository.findByIdAndUserId(id, userId)
                 .orElseThrow(() -> new TaskNotFoundException(id));
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setPriority(request.getPriority());
        entity.setDueDate(request.getDueDate());
        TaskEntity updated = taskRepository.save(entity);
        return TaskResponse.fromEntity(updated);
    }

    /**
     * Este metodo se encarga de eliminar una tarea dentro de la aplicacion web
     */
    @Override
    public void delete(Long id, Long userId){
         TaskEntity entity = taskRepository.findByIdAndUserId(id, userId)
                 .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(entity);
    }
}
