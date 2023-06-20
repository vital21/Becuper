package com.example.alhohelp.controller;

import com.example.alhohelp.entity.GeneralAccess;
import com.example.alhohelp.entity.User;
import com.example.alhohelp.repository.GeneralAccessRepository;
import com.example.alhohelp.repository.UserRepository;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Controller
public class DirVidController {
    private static final String UPLOAD_DIR = "C:\\upload";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GeneralAccessRepository generalAccessRepository;
    @GetMapping("/dirOpen/{dirName}")
    public String dirOpen(Model model, @PathVariable String dirName, @AuthenticationPrincipal User user , HttpSession session, HttpServletResponse response){
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        uploadDirectory = uploadDirectory + "/" + dirName;
        List<String> folderNames = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> fileDate=new ArrayList<>();
        List<String> folderDate=new ArrayList<>();
        List<Long> fileSize= new ArrayList<>();
        List<Long> folderSize = new ArrayList<>();
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        File directory = new File(uploadDirectory);
        if (directory.isDirectory()) {

            File[] files = directory.listFiles();


            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderDate.add(dateFile(file));
                        folderSize.add(folderSize(file)/1024);
                        folderNames.add(file.getName());

                    } else {
                        fileDate.add(dateFile(file));
                        fileSize.add(folderSize(file)/1024);
                        fileNames.add(file.getName());
                    }
                }
            }
        }
        String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
        if(user.getSecurityHes().contains(hash)) {
            model.addAttribute("prov",true);
        }
        else model.addAttribute("prov",false);
        String uploadDir = uploadDirectory.replaceFirst("C:\\\\upload/", "");
        model.addAttribute("dirs",folderNames);
        model.addAttribute("files",fileNames);
        model.addAttribute("path",uploadDir);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        session.setAttribute("uploadDirectory", uploadDirectory);
        return "directories";
    }
    @GetMapping("/dirBack")
    public String dirBack(Model model, @AuthenticationPrincipal User user, HttpSession session) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        int lastIndex = uploadDirectory.lastIndexOf("/");
        uploadDirectory = uploadDirectory.substring(0, lastIndex);
        if(uploadDirectory.equals(UPLOAD_DIR+"/"+user.getUsername()+"/Dir")){
            return "redirect:/files";
        }
        List<String> folderNames = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> fileDate=new ArrayList<>();
        List<String> folderDate=new ArrayList<>();
        List<Long> fileSize= new ArrayList<>();
        List<Long> folderSize = new ArrayList<>();
        File directory = new File(uploadDirectory);


        if (directory.isDirectory()) {

            File[] files = directory.listFiles();


            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderDate.add(dateFile(file));
                        folderSize.add(folderSize(file)/1024);
                        folderNames.add(file.getName());
                    } else {
                        fileDate.add(dateFile(file));
                        fileSize.add(folderSize(file)/1024);
                        fileNames.add(file.getName());
                    }
                }
            }
        }
        String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
        if(user.getSecurityHes().contains(hash)) {
            model.addAttribute("prov",true);
        }
        else model.addAttribute("prov",false);
        String uploadDir = uploadDirectory.replaceFirst("C:\\\\upload/", "");
        model.addAttribute("dirs", folderNames);
        model.addAttribute("files", fileNames);
        model.addAttribute("path",uploadDir);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        session.setAttribute("uploadDirectory", uploadDirectory);
        return "directories";
    }
    @GetMapping("/dirDelete/{dirName}")
    public String deleteDir(@PathVariable String dirName,@AuthenticationPrincipal User user,HttpSession session){
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        uploadDirectory=uploadDirectory+"/"+dirName;
        File file = new File(uploadDirectory);
        try {
            deleteRecursive(file);
            List<GeneralAccess> generalAccesses = generalAccessRepository.queryByUserId(user.getId());
            deleteFromBd(generalAccesses,uploadDirectory);
            System.out.println("Папка успешно удалена.");
            session.setAttribute("uploadDirectory", uploadDirectory);
            String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
            user.setSecurityHes(hash);
            userRepository.save(user);
            return "redirect:/dirBack";
        } catch (Exception e) {
            System.out.println("Ошибка при удалении папки: " + e.getMessage());
            session.setAttribute("uploadDirectory", uploadDirectory);
            return "redirect:/dirBack";
        }
    }
    @PostMapping("/newDir")
    public String createFolder(@RequestParam("FolderName") String folderName,@AuthenticationPrincipal User user,HttpSession session) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        File folder = new File(uploadDirectory, folderName);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                System.out.println("Папка успешно создана.");
                uploadDirectory = uploadDirectory + "/" + folderName;
                session.setAttribute("uploadDirectory", uploadDirectory);
                String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
                user.setSecurityHes(hash);
                userRepository.save(user);
                return "redirect:/dirBack";
            } else {
                System.out.println("Ошибка при создании папки.");

            }
        } else {
            int ind=1;
            System.out.println("Папка с таким названием уже существует.");
            while (ind !=100) {
                File folders = new File(uploadDirectory, folderName + "(" + ind + ")");
                ind++;
                if (folders.mkdirs()) {
                    System.out.println("Папка успешно создана.");
                    uploadDirectory = uploadDirectory + "/" + folderName;
                    session.setAttribute("uploadDirectory", uploadDirectory);
                    String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
                    user.setSecurityHes(hash);
                    return "redirect:/dirBack";
                } else {
                    System.out.println("Ошибка при создании папки.");
                }
            }

        }

        return "redirect:/files";
    }
    @GetMapping("/downloadDirs/{dirName}")
    public ResponseEntity<Resource> DirDownload(@PathVariable String dirName, HttpSession session,@AuthenticationPrincipal User user) throws UnsupportedEncodingException {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");

            uploadDirectory = uploadDirectory + "/" + dirName;
            String zipFileName = dirName + ".zip";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                File fileSource = new File(uploadDirectory);
                addDirectory(zos, fileSource, dirName);
                zos.finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            byte[] zipBytes = baos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(zipBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"; filename*=UTF-8''" + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.toString()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(resource);
    }
    @GetMapping("/uploadFilesInFolder")
    public String uploadInFolder(){
        return "uploadInFolder";
    }
    @PostMapping("/uploadFilesInFolder")
    public String uploadFilesInDirectory(@RequestParam("files") List<MultipartFile> files,
                                         @AuthenticationPrincipal User user,
                                         RedirectAttributes redirectAttributes, HttpSession session){
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");

        if (files.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select files to upload");
            uploadDirectory=uploadDirectory+"/"+"dirs";
            session.setAttribute("uploadDirectory",uploadDirectory);
            return "redirect:/dirBack";
        }

        try {
            for (MultipartFile file : files) {
                String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                Path targetLocation = Path.of(uploadDirectory).resolve(fileName);

                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
                user.setSecurityHes(hash);
                userRepository.save(user);
            }

            redirectAttributes.addFlashAttribute("message", "Файл успешно загружен");
        } catch (IOException ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Ошибка загрузки");
        }
        uploadDirectory=uploadDirectory+"/"+"dirs";
        session.setAttribute("uploadDirectory",uploadDirectory);
        return "redirect:/dirBack";

    }
    @GetMapping("/uploadFilesInDir")
    public String uploadDirFile(){
        return "uploadInFolder";
    }
    @PostMapping("/uploadFilesInDir")
    public String uploadFiles(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes, HttpSession session) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:/uploadFilesInDir";
        }

        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path targetLocation = Path.of(uploadDirectory).resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            uploadDirectory=uploadDirectory+"/"+fileName;
            session.setAttribute("uploadDirectory",uploadDirectory);
            redirectAttributes.addFlashAttribute("message", "Файлы успешно загружены");
            String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
            user.setSecurityHes(hash);
            userRepository.save(user);
        } catch (IOException ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Ошибка загрузки");
        }

        return "redirect:/dirBack";
    }
    @PostMapping("/uploadZipDir")
    public String extractArchive(@RequestParam("file") MultipartFile file,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes, HttpSession session) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select an archive file to extract");
            return "redirect:/uploadFilesInDir";
        }

        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path targetLocation = Path.of(uploadDirectory).resolve(fileName);

            // Сохраняем архивный файл
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Создаем папку с именем архива
            String archiveDirectoryName = getArchiveDirectoryName(fileName);
            Path archiveDirectory = Path.of(uploadDirectory).resolve(archiveDirectoryName);
            Files.createDirectories(archiveDirectory);

            // Разархивируем файл в папку с именем архива
            extractFiles(targetLocation, archiveDirectory);
            Files.delete(targetLocation);
            uploadDirectory=uploadDirectory+"/"+fileName;
            session.setAttribute("uploadDirectory",uploadDirectory);
            redirectAttributes.addFlashAttribute("message", "Archive extracted successfully");
            String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
            user.setSecurityHes(hash);
            userRepository.save(user);
        } catch (IOException ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Failed to extract archive");
            return "redirect:/dirBack";
        }

        return "redirect:/dirBack";
    }
    @GetMapping("/filesInDirDownload/{fileName}")
    public ResponseEntity<Resource> fileInDirDownload(@PathVariable String fileName, HttpSession session,
                                                      @AuthenticationPrincipal User user){
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        uploadDirectory=uploadDirectory+"/"+fileName;

        File file = new File(uploadDirectory);
        if (file.exists()) {
            String originalExtension = ".txt";
            if (fileName.toLowerCase().endsWith(originalExtension)) {
                Resource resource = new FileSystemResource(file);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(fileName, StandardCharsets.UTF_8) + "\"");
                String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
                user.setSecurityHes(hash);
                userRepository.save(user);
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } else {
                Resource resource =  new FileSystemResource(file);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(fileName, StandardCharsets.UTF_8) + "\"")
                        .body(resource);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/filesInDirDelete/{fileName}")
    public String deleteFileInFolder(@PathVariable String fileName, HttpSession session, @AuthenticationPrincipal User user) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        String filePath = uploadDirectory + "/" + fileName;

        try {
            Path fileToDelete = Path.of(filePath);
            Files.deleteIfExists(fileToDelete);
            List<GeneralAccess> generalAccesses = generalAccessRepository.queryByUserId(user.getId());
            deleteFromBd(generalAccesses,filePath);
            String hash = generateDirectoryHash(UPLOAD_DIR+"/"+user.getUsername());
            user.setSecurityHes(hash);
            userRepository.save(user);
            uploadDirectory=uploadDirectory+"/"+fileName;
            session.setAttribute("uploadDirectory",uploadDirectory);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/dirBack";
    }

    private void deleteFromBd(List<GeneralAccess> generalAccesses,String filePath){
        for(GeneralAccess i : generalAccesses){
            if(i.getAddress().contains(filePath)){
                generalAccessRepository.deleteByAddress(i.getAddress());
            }
        }
    }
    private void addDirectory(ZipOutputStream zos, File fileSource, String parentEntryPath) throws IOException {
        if (fileSource.isDirectory()) {
            File[] files = fileSource.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    String entryName = parentEntryPath + "/" + file.getName();
                    if (file.isDirectory()) {
                        addDirectory(zos, file, entryName);
                    } else {
                        FileInputStream fis = new FileInputStream(file);
                        zos.putNextEntry(new ZipEntry(entryName));

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }

                        zos.closeEntry();
                        fis.close();
                    }
                }
            } else {
                zos.putNextEntry(new ZipEntry(parentEntryPath + "/"));
                zos.closeEntry();
            }
        }
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
    private void extractFiles(Path zipFilePath, Path targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilePath.toFile())), Charset.forName("CP866"))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryFilePath = targetDirectory.resolve(entry.getName());
                if (entry.isDirectory()) {
                    // Создаем директорию
                    Files.createDirectories(entryFilePath);
                } else {
                    // Извлекаем файл
                    Files.createDirectories(entryFilePath.getParent());
                    Files.copy(zis, entryFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private String getArchiveDirectoryName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        return fileName;
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

