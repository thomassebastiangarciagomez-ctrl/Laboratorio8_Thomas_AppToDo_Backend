package edu.eci.dosw.todo.dto;
import edu.eci.dosw.todo.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;


public class TaskCreateRequest {
    /**
     * Se añaden los atributos correspondientes junto con las validaciones las cuales nos permiten saber si se
     * entregaron los valores de la forma esperada, estas validaciones son NotBlank, NotNull y Size.
     */
    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 120, message = "El titulo no puede superar los 120 caracteres")
    private String title;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String description;

    @NotNull(message = "La prioridad es obligatoria")
    private TaskPriority priority;

    private LocalDate dueDate;

    /**
     * Seccion de geters y seters en el codigo para poder modificar la informacion
     */

    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }

    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }

    public TaskPriority getPriority(){ return priority; }
    public void setPriority(TaskPriority priority){ this.priority = priority; }

    public LocalDate getDueDate(){ return dueDate; }
    public void setDueDate(LocalDate dueDate){ this.dueDate = dueDate; }
}

