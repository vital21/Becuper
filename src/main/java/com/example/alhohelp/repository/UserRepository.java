package com.example.alhohelp.repository;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String name);
    User findAllByUsernameNotNull();
    void deleteById(Long id);
}
