package com.example.alhohelp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.PushBuilder;
import java.util.Map;

@Controller
public class MainController {
    @GetMapping("/")
    public String home(Map<String,Object> model){
        return "home";
    }





}
