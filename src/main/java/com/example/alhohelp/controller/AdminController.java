package com.example.alhohelp.controller;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import com.example.alhohelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
@GetMapping("users")
    public String usersList(Model model){
        model.addAttribute("users",userRepository.findAll());
    return "user-list";
}
@GetMapping("usersRedact")
public String userRed(@PathVariable String userName){

    return "userRedact";
}

@PostMapping("usersRedact")
    public String userRedact(@RequestParam("userName") String userName, Model
                             model){
    model.addAttribute("user",userRepository.findByUsername(userName));
    return "userRedact";
}
    @PostMapping("usersRedacts")
    public String userRedact(@RequestParam("newUserName") String newUserName, @RequestParam("userName") String userName, @RequestParam("password") String password, @RequestParam("role") Set<Role> roles, Model
            model){
        User user = userRepository.findByUsername(userName);
        if (user != null) {
            user.setUsername(newUserName);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.getRoles().clear();
            for (Role role : roles) {
                user.getRoles().add(role);
            }
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found");
        }
        model.addAttribute("user",userRepository.findByUsername(newUserName));
        return "userRedact";
    }
    @PostMapping ("/deleteUser")
    public String deleteUser(@RequestParam("userId") Long userId, Model model) {
        userRepository.deleteById(userId);
        model.addAttribute("users",userRepository.findAll());
        return "user-list";
    }

}
