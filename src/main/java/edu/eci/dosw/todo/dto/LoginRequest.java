package edu.eci.dosw.todo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    
    @NotBlank(message = "Debe indicar un email")
    @Email(message = "Ingrese un correo valido")
    @Size(max = 100, message = "El tamaño maximo del correo es de 100 caracteres")
    private String email;

    @NotBlank(message = "Indique una contraseña")
    @Size(max = 72, message = "El tamaño maximo de la contraseña es de 72 caracteres")
    private String password;


    
    public String getEmail(){ return this.email; }
    public void setEmail(String email){ this.email = email; }

    public String getPassword(){ return this.password; }
    public void setPassword(String password){ this.password = password; }
}
