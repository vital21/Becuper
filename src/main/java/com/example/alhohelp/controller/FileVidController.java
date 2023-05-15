package com.example.alhohelp.controller;

import com.example.alhohelp.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Controller
public class FileVidController {
    @GetMapping("/files")
    public String getFileList(Model model, @AuthenticationPrincipal User user) {

        String uploadDir = "C:\\upload";
        File folder = new File(uploadDir+"/"+user.getUsername());
        List<String> files = Arrays.asList(folder.list());
        model.addAttribute("files", files);
        return "file-list";
    }
}
