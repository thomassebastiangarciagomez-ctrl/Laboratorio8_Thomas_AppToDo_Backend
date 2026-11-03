package edu.eci.dosw.todo.exception;

public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}