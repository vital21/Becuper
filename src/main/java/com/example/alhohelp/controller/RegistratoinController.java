package com.example.alhohelp.controller;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import com.example.alhohelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Controller
public class RegistratoinController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    private static final String UPLOAD_DIR = "C:\\upload";

    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    public String addUser(User user, RedirectAttributes redirectAttributes) throws IOException {
        User userFromDb = userRepository.findByUsername(user.getUsername());

        if (userFromDb != null) {
            redirectAttributes.addFlashAttribute("message", "User exists!");
            return "login";
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        Path uploadDir = Path.of(UPLOAD_DIR+"/"+user.getUsername());
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        return "redirect:/login";
    }
}
