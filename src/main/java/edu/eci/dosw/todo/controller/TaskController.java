package edu.eci.dosw.todo.controller;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.service.TaskService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }

    /**
     * Endpoint encargado de mostrar todos las tareas que estan publicadas en este momento, esta retorna una lista
     * de TaskResponse de todas las tareas publicadas
     * 
     * @return
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(taskService.findAll(Long.valueOf(jwt.getSubject())));
    }

    /**
     * Endpoint encargado de mostrar una tarea en especifico que este publicada previamente, en caso de que no exista
     * lanza un error de tarea no encontrada.
     * 
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(taskService.findById(id, Long.valueOf(jwt.getSubject())));
    }

    /**
     * Endpoint que se encarga de publicar una nueva tarea en la base de datos
     * 
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request, @AuthenticationPrincipal Jwt jwt){
        TaskResponse created = taskService.create(request, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Endpoint que se encarga de actualizar una tarea que ya este publicada previamente, en caso de que no exista lanza
     * un error de tarea no encontrada
     * 
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(taskService.update(id,request, Long.valueOf(jwt.getSubject())));
    }

    /**
     * Endpoint que se encarga de eliminar una tarea publicada previamente, en caso de que no exista lanza un error de
     * tarea no encontrada.
     * 
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<TaskResponse> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        taskService.delete(id, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
