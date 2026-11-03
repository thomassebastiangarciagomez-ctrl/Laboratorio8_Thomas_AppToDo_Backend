package edu.eci.dosw.todo.dto;

import java.util.Map;

public record AdminSummaryResponse(long totalUsers, long totalTasks, Map<String, Long> tasksByStatus) {}
