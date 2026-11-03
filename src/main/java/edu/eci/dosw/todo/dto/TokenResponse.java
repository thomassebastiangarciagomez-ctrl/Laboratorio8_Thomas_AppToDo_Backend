package edu.eci.dosw.todo.dto;

public record TokenResponse(String tokenType, String accessToken, long expiresIn) {}
