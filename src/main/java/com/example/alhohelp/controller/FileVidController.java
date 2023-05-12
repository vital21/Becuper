package com.example.alhohelp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Controller
public class FileVidController {
    @GetMapping("/files")
    public String getFileList(Model model) {
        String uploadDir = "C:\\upload";
        File folder = new File(uploadDir);
        File[] listOfFiles = folder.listFiles();
        List<String> files = Arrays.asList(folder.list());
        model.addAttribute("files", files);
        return "file-list";
    }
}
