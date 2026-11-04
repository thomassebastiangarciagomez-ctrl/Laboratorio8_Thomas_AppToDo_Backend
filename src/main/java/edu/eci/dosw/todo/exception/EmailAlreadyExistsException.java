package edu.eci.dosw.todo.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    
    public EmailAlreadyExistsException() {
        super("El correo ya está registrado");
    }
}

