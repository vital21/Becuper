package com.example.alhohelp.controller;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import com.example.alhohelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.PushBuilder;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

@Controller
public class MainController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @GetMapping("/")
    public String home(Map<String,Object> model){
        return "home";
    }
    @GetMapping("usersRedactUs")
    public String userRed(){
        return "userRedactUser";
    }

    @GetMapping("usersRedactUser")
    public String userRedact(@AuthenticationPrincipal User user, Model
            model){
        String UPLOAD_DIR = "C:\\upload/"+user.getUsername();
        File folder = new File(UPLOAD_DIR);
        long folderSize = getFolderSize(folder);
        double sizeInMB = (double) folderSize / (1024 * 1024);
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedSize = df.format(sizeInMB);
        model.addAttribute("size",formattedSize);
        model.addAttribute("user",user);
        return "userRedactUser";
    }
    @PostMapping("usersRedactUsers")
    public String userRedact(@RequestParam("newUserName") String newUserName, @RequestParam("userName") String userName, @RequestParam("newPassword") String newPassword,@RequestParam("OldPassword") String OldPassword, Model model,@AuthenticationPrincipal User user){

        if (user != null && passwordEncoder.matches(OldPassword, user.getPassword())) {
            user.setUsername(newUserName);
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            String folderPath = "C:\\upload/"+userName;
            String newFolderName=newUserName;
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                String parentPath = folder.getParent();
                String newFolderPath = parentPath + File.separator + newFolderName;
                File newFolder = new File(newFolderPath);
                if (newFolder.exists()) {
                    System.out.println("Папка с новым именем уже существует.");
                } else {

                    if (folder.renameTo(newFolder)) {
                        System.out.println("Папка успешно переименована.");
                    } else {
                        System.out.println("Не удалось переименовать папку.");
                    }
                }
            } else {
                System.out.println("Указанная папка не существует.");
            }
        } else {
            throw new IllegalArgumentException("User not found");
        }
        model.addAttribute("user",userRepository.findByUsername(newUserName));
        return "userRedactUser";
    }
    public static long getFolderSize(File folder) {
        if (folder == null || !folder.isDirectory()) {
            throw new IllegalArgumentException("Not a valid folder");
        }

        long size = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getFolderSize(file);
                }
            }
        }

        return size;
    }
}
