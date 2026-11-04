package edu.eci.dosw.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    
    @NotBlank(message = "Tiene que ingresar su nombre")
    @Size(min = 2, max = 100, message = "El nombre tiene que tener entre 2 a 100 caracteres")
    private String name;

    @NotBlank(message = "El correo no puede quedar vacio")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Utilice un correo valido")
    @Size(max = 100, message = "El correo no debe superar los 100 caracteres")
    private String email;

    @NotBlank(message = "Debe ingresar una contraseña")
    @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 a 72 caracteres")
    private String password;


    public String getName(){ return this.name; }
    public void setName(String name){ this.name = name; }

    public String getEmail(){ return this.email; }
    public void setEmail(String email){ this.email = email; }

    public String getPassword(){ return this.password; }
    public void setPassword(String password){ this.password = password; }
}
