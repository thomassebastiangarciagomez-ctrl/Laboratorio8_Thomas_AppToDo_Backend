package edu.eci.dosw.todo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.service.AdminService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    private AdminService adminService;

    public AdminController(AdminService adminService){
        this.adminService = adminService;
    }

    /**
     * 
     * @return
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminService.getSummary());
    }

}
