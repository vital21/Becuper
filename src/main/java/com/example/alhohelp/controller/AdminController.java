package com.example.alhohelp.controller;

import com.example.alhohelp.entity.Role;
import com.example.alhohelp.entity.User;
import com.example.alhohelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Optional;
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

    String UPLOAD_DIR = "C:\\upload/"+userName;
    File folder = new File(UPLOAD_DIR);
    long folderSize = getFolderSize(folder);
    double sizeInMB = (double) folderSize / (1024 * 1024);
    DecimalFormat df = new DecimalFormat("#.##");
    String formattedSize = df.format(sizeInMB);
    model.addAttribute("size",formattedSize);
    model.addAttribute("user",userRepository.findByUsername(userName));
    return "userRedact";
}
    @PostMapping("usersRedacts")
    public String userRedact(@RequestParam("newUserName") String newUserName, @RequestParam("userName") String userName, @RequestParam(name = "isButtonPressed", defaultValue = "false") Boolean isButtonPressed,@RequestParam("role") Set<Role> roles, Model model){
        User user = userRepository.findByUsername(userName);
        if (user != null) {
            user.setUsername(newUserName);
           if(isButtonPressed){user.setPassword(passwordEncoder.encode(newUserName));}
            user.getRoles().clear();
            for (Role role : roles) {
                user.getRoles().add(role);
            }
            userRepository.save(user);
            String folderPath = "C:\\upload/"+userName;
            String newFolderName=newUserName;
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                String parentPath = folder.getParent();
                String newFolderPath = parentPath + File.separator + newFolderName;

                File newFolder = new File(newFolderPath);

                // Проверяем, существует ли папка с новым именем
                if (newFolder.exists()) {
                    System.out.println("Папка с новым именем уже существует.");
                } else {
                    // Переименовываем папку
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
        return "userRedact";
    }
    @PostMapping ("/deleteUser")
    public String deleteUser(@RequestParam("userId") Long userId, Model model) {

        User user = userRepository.getById(userId);
        String username = user.getUsername();
        String uploadDirectory="C:\\upload/"+username;
        File file = new File(uploadDirectory);
        try {
            deleteRecursive(file);
            System.out.println("Папка успешно удалена.");

        } catch (Exception e) {
            System.out.println("Ошибка при удалении папки: " + e.getMessage());

            return "user-list";
        }

        userRepository.deleteById(userId);
        model.addAttribute("users",userRepository.findAll());
        return "user-list";
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
    private void deleteRecursive(File file)throws Exception{
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursive(f);
                }
            }
        }

        if (!file.delete()) {
            throw new Exception("Не удалось удалить файл: " + file.getAbsolutePath());
        }
    }

}
