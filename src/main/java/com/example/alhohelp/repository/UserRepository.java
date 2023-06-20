package com.example.alhohelp.repository;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String name);
    @Query(value = "SELECT * FROM users where id = ?", nativeQuery = true)
    User findById(int id);
    void deleteById(Long id);
}
