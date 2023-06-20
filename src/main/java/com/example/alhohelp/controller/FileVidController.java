package com.example.alhohelp.controller;
import com.example.alhohelp.entity.User;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class FileVidController {
    private static final String UPLOAD_DIR = "C:\\upload";
    private static String UPLOAD_DIRECTORIES="C:\\upload";
    @GetMapping("/files")
    public String getFileList(Model model, @AuthenticationPrincipal User user,HttpSession session) {
        File folder = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files");
        List<String> files = Arrays.asList(folder.list());

        File dirFolder = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Dir");
        List<String> dirs = Arrays.asList(dirFolder.list());
        if(user.getRoles().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))){
            model.addAttribute("isAdmin",true);
        }
        else {
            model.addAttribute("isAdmin",false);
        }
        List<String> fileDate=new ArrayList<>();
        List<String> folderDate=new ArrayList<>();
        List<Long> fileSize= new ArrayList<>();
        List<Long> folderSize = new ArrayList<>();
        File[] folders = dirFolder.listFiles();
        File[] fileses = folder.listFiles();
        for(File i : fileses){
            fileDate.add(dateFile(i));
            fileSize.add(folderSize(i)/1024);
        }
        for(File i : folders){
            folderDate.add(dateFile(i));
            folderSize.add(folderSize(i)/1024);
        }
        String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
        if(user.getSecurityHes().contains(hash)) {
            model.addAttribute("prov",true);
        }
        else model.addAttribute("prov",false);
        model.addAttribute("files", files);
        model.addAttribute("dirs",dirs);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        model.addAttribute("path",UPLOAD_DIRECTORIES + "/" + user.getUsername() + "/Dir");
        session.setAttribute("uploadDirectory", UPLOAD_DIRECTORIES + "/" + user.getUsername() + "/Dir");
        return "file-list";
    }
    @GetMapping("/filesVersions/{fileName}")
    public String getFileVersions(Model model,@PathVariable String fileName, @AuthenticationPrincipal User user){
        File folder = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files"+"/"+fileName);
        List<String> files = Arrays.asList(folder.list());
        model.addAttribute("files", files);
        return "file-version-list";
    }
    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, @AuthenticationPrincipal User user) {
        String cleanedFilename;
        String[] parts = fileName.split("_", 2);
            cleanedFilename = parts[1];
        String filePath = UPLOAD_DIR + "/" + user.getUsername()+"/Files" + "/" + cleanedFilename+"/"+fileName;
        File file = new File(filePath);
        if (file.exists()) {
            String originalExtension = ".txt";
            if (fileName.toLowerCase().endsWith(originalExtension)) {
                Resource resource = new FileSystemResource(file);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(cleanedFilename, StandardCharsets.UTF_8) + "\"");

                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } else {
                Resource resource =  new FileSystemResource(file);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(cleanedFilename, StandardCharsets.UTF_8) + "\"")
                        .body(resource);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/files1/{fileName}")
    public ResponseEntity<Resource> downloadLastFile(@PathVariable String fileName, @AuthenticationPrincipal User user) {
        String lastFileName = nameLastFile(user, fileName);
        String cleanedFilename = lastFileName.replaceAll("\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}_", "");
        String filePath = UPLOAD_DIR + "/" + user.getUsername() +"/Files"+ "/" + fileName + "/" + lastFileName;
        File file = new File(filePath);
        if (file.exists()) {
            String originalExtension = ".txt";
            if (lastFileName.toLowerCase().endsWith(originalExtension)) {
                Resource resource = new FileSystemResource(file);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(cleanedFilename, StandardCharsets.UTF_8) + "\"");

                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } else {
                Resource resource =  new FileSystemResource(file);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(cleanedFilename, StandardCharsets.UTF_8) + "\"")
                        .body(resource);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/upload")
    public String provideUploadInfo() {

        return "upload";
    }

    @PostMapping("/uploadFiles")
    public String uploadFiles(@RequestParam("file") MultipartFile file,
                             @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:/upload";
        }
        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path fileNameCopy= Path.of(file.getOriginalFilename());
            Path uploadDir = Path.of(UPLOAD_DIR+"/"+user.getUsername()+"/Files");
            Path uploadCopyFile = Path.of(uploadDir+"/"+fileNameCopy);


            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss");
            String formattedDateTime = currentTime.format(formatter);
            String newFileName = formattedDateTime + "_" + fileName;


            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            if(!Files.exists(uploadCopyFile)){
                Files.createDirectories(uploadCopyFile);
            }
            Path targetLocation = uploadCopyFile.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            redirectAttributes.addFlashAttribute("message", "Файл успешно загружен");
        } catch (IOException ex) {
            ex.printStackTrace();

            redirectAttributes.addFlashAttribute("message", "Ошибка загрузки");
        }
        return "redirect:/upload";
    }
    @GetMapping ("/filesDelete/{fileName}")
    public String DeleteAllFilesInFolder(@PathVariable String fileName,@AuthenticationPrincipal User user,RedirectAttributes redirectAttributes){
        File folderName=new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files"+"/"+fileName);
        File folder = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files");
        List<String> files = Arrays.asList(folder.list());
        try {
            deleteRecursive(folderName);
            System.out.println("Папка успешно удалена.");
            files = Arrays.asList(folder.list());
            redirectAttributes.addFlashAttribute("files", files);
        } catch (Exception e) {
            System.out.println("Ошибка при удалении папки: " + e.getMessage());
            redirectAttributes.addFlashAttribute("files", files);
        }
        return "redirect:/files";
    }
    @GetMapping("/fileDelete/{fileName}")
    public String deleteFile(@PathVariable String fileName,@AuthenticationPrincipal User user,RedirectAttributes redirectAttributes) throws IOException {
        String cleanedFilename = fileName.replaceAll("\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}_", "");
        File file = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files"+"/"+cleanedFilename+"/"+fileName);
        File folder=new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files"+"/"+cleanedFilename);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("Файл успешно удален.");

            } else {
                System.out.println("Не удалось удалить файл.");

            }
        } else {
            System.out.println("Файл не существует.");
        }
        List<String> files = Arrays.asList(folder.list());
        if(files.size()==0){
            FileUtils.deleteDirectory(folder);
            return "redirect:/files";
        }
        return "redirect:/filesVersions/"+cleanedFilename;
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
    private String nameLastFile(User user,String fileName){
        File folder = new File(UPLOAD_DIR+"/"+user.getUsername()+"/Files"+"/"+fileName);
        List<String> files = Arrays.asList(folder.list());
        Collections.sort(files, Comparator.reverseOrder());
        return files.get(0);
    }
    private String dateFile(File file){
        if (file.exists()) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                FileTime creationTime = attrs.creationTime();
                Date creationDate = new Date(creationTime.toMillis());

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = dateFormat.format(creationDate);
                return formattedDate;
            } catch (Exception e) {
                System.out.println("Ошибка при получении даты создания файла: " + e.getMessage());
            }
        } else {
            System.out.println("Файл не существует.");
            return "Нет информации по файлу";
        }
        return "Нет информации";
    }
    private long folderSize(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                return file.length();
            } else if (file.isDirectory()) {
                long size = 0;
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            size += f.length();
                        } else if (f.isDirectory()) {
                            size += folderSize(f);
                        }
                    }
                }
                return  size ;
            }
        } else {
            System.out.println("Файл или папка не существует.");
        }
        return 0;
    }
    public static String generateDirectoryHash(String directoryPath) {
        StringBuilder sb = new StringBuilder();
        traverseDirectory(new File(directoryPath), sb);
        String directoryString = sb.toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(directoryString.getBytes());

            StringBuilder hashStringBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hashStringBuilder.append('0');
                hashStringBuilder.append(hex);
            }

            return hashStringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    private static void traverseDirectory(File directory, StringBuilder sb) {
        if (directory.isDirectory()) {
            sb.append(directory.getName()).append("\n");
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    traverseDirectory(file, sb);
                }
            }
        } else {
            sb.append(directory.getName()).append("\n");
        }
    }








}


