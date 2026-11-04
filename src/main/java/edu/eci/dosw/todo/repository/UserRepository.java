package edu.eci.dosw.todo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.eci.dosw.todo.entity.UserEntity;

@Repository 
public interface UserRepository extends JpaRepository<UserEntity, Long>{
    
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
    
}
