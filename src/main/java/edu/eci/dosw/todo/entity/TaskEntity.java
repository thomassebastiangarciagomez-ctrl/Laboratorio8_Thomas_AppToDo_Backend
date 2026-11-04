package edu.eci.dosw.todo.entity;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class TaskEntity {
    
    /**
     * Se toman como escriben como atributos de la clase los cuales tienen banderas que indican su comportamiento y 
     * que vienen desde la tabla tasks en la base de datos
     */
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Todos los metodos necesarios para poder acceder a los valores dentro de la tabla debido a que todos
     * los atributos anteriormente creados son private (Gets).
     */

    public Long getId(){ return id; }
    public void setId(Long id ) {this.id = id; }

    public String getTitle() {return title; }
    public void setTitle(String title){ this.title = title; }

    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }

    public TaskStatus getStatus(){ return status; }
    public void setStatus(TaskStatus status){ this.status = status; }

    public TaskPriority getPriority(){ return priority; }
    public void setPriority(TaskPriority priority){ this.priority = priority; }

    public LocalDate getDueDate(){ return dueDate; }
    public void setDueDate(LocalDate dueDate){ this.dueDate = dueDate; }

    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
}
