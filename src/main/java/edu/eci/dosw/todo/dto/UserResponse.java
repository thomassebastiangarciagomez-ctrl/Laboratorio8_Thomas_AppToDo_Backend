package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.entity.UserRole;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private UserRole role;

    /**
     * 
     * @param entity
     * @return
     */
    public static UserResponse fromEntity(UserEntity entity){
        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setEmail(entity.getEmail());
        response.setRole(entity.getRole());
        return response;
    }

    public Long getId(){ return this.id; }
    public void setId(Long id){ this.id = id; }

    public String getName(){ return this.name; }
    public void setName(String name){ this.name = name; }

    public String getEmail(){ return this.email; }
    public void setEmail(String email){ this.email = email; }

    public UserRole getRole(){ return this.role; }
    public void setRole(UserRole role){ this.role = role; }
}
