package edu.eci.dosw.todo.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;

@Service 
public class AdminServiceImpl implements AdminService{
    
    private UserRepository userRepository;
    private TaskRepository taskRepository;


    public AdminServiceImpl(UserRepository userRepository, TaskRepository taskRepository){
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * 
     */
    @Override
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary(){
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status.name(), taskRepository.countByStatus(status));
        }
        return new AdminSummaryResponse(userRepository.count(), taskRepository.count(), byStatus);
    }
}
