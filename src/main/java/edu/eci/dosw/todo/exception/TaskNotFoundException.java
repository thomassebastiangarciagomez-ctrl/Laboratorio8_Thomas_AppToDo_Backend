package edu.eci.dosw.todo.exception;

public class TaskNotFoundException extends RuntimeException{
    
    public static final String NOT_FOUND = "NO SE ENCONTRO EL RECURSO CON LA ID PROPUESTA ";

    public TaskNotFoundException(Long id){
        super(NOT_FOUND + id);
    }
}
