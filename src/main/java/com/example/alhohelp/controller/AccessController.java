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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class AccessController {
    @Autowired
    GeneralAccessRepository generalAccessRepository;
    @Autowired
    private UserRepository userRepository;
    private static final String UPLOAD_DIR = "C:\\upload";

@PostMapping("/filesAccess/{fileName}")
    public String access(@PathVariable String fileName, @RequestParam("username") String username , @AuthenticationPrincipal User user, HttpSession session){
    GeneralAccess generalAccess = new GeneralAccess();
    String uploadDirectory = (String) session.getAttribute("uploadDirectory");
    generalAccess.setAddress(uploadDirectory+"/"+fileName);
    generalAccess.setUserId(user.getId());
    User us= userRepository.findByUsername(username);
    generalAccess.setUseUser(us.getId());
    generalAccessRepository.save(generalAccess);
    session.setAttribute("uploadDirectory",uploadDirectory+"/"+fileName);
return "redirect:/dirBack";

}
    @PostMapping("/filesAccessVersion/{fileName}")
    public String accessVersion(@PathVariable String fileName, @RequestParam("username") String username , @AuthenticationPrincipal User user, HttpSession session){
        GeneralAccess generalAccess = new GeneralAccess();
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        String cleanedFilename = fileName.replaceAll("\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}_", "");
        generalAccess.setAddress(UPLOAD_DIR+"/"+user.getUsername()+"/"+"Files/"+cleanedFilename+"/"+fileName);
        generalAccess.setUserId(user.getId());
        User us= userRepository.findByUsername(username);
        generalAccess.setUseUser(us.getId());
        generalAccessRepository.save(generalAccess);
        session.setAttribute("uploadDirectory",uploadDirectory+"/"+fileName);
        return "redirect:/dirBack";

    }
@GetMapping("/filesAccess")
    public String acc(@AuthenticationPrincipal User user, Model model,HttpSession session){
    List<String> folderPaths = new ArrayList<>();

    List<GeneralAccess> generalAccesses= generalAccessRepository.queryByUsUser(user.getId());
    List<File> files = new ArrayList<>();
    List<String> autFile= new ArrayList<>();
    List<String> autFolder= new ArrayList<>();
    List<Integer> useridFolder= new ArrayList<>();
    List<Integer> useridFile= new ArrayList<>();
    List<Integer> idPathFolder=new ArrayList<>();
    List<Integer> idPatFile = new ArrayList<>();
    List<Integer> path = new ArrayList<>();
    for(GeneralAccess i : generalAccesses){
        folderPaths.add(i.getAddress());
        path.add(Math.toIntExact(i.getId()));
    }
    for(String str :folderPaths){
        File file = new File(str);
        files.add(file);
    }
    List<String> folderNames = new ArrayList<>();
    List<String> fileNames = new ArrayList<>();
    List<String> fileDate=new ArrayList<>();
    List<String> folderDate=new ArrayList<>();
    List<Long> fileSize= new ArrayList<>();
    List<Long> folderSize = new ArrayList<>();

        if (files != null) {
            int i=0;
            for (File file : files) {
                if (file.isDirectory()) {

                    useridFolder.add(Math.toIntExact(generalAccesses.get(i).getUserId()));
                    folderDate.add(dateFile(file));
                    folderSize.add(folderSize(file)/1024);
                    idPathFolder.add(path.get(i));
                    folderNames.add(file.getName());
                    i++;
                } else {
                    useridFile.add(Math.toIntExact(generalAccesses.get(i).getUserId()));
                    fileDate.add(dateFile(file));
                    fileSize.add(folderSize(file)/1024);
                    fileNames.add(file.getName());
                    i++;
                }
            }
        }
        for(int i: useridFile){
           User users=userRepository.findById(i);
            autFile.add(users.getUsername());
        }
    for(int i: useridFolder){
        User users=userRepository.findById(i);
        autFolder.add(users.getUsername());
    }
        model.addAttribute("prov",true);
    model.addAttribute("dirs",folderNames);
    model.addAttribute("files",fileNames);
    model.addAttribute("folderDate",folderDate);
    model.addAttribute("fileDate",fileDate);
    model.addAttribute("folderSize",folderSize);
    model.addAttribute("fileSize",fileSize);
    model.addAttribute("autFile",autFile);
    model.addAttribute("pathFolder", idPathFolder);
    model.addAttribute("autFolder",autFolder);
    return "access";

}
@GetMapping("/filesInAccessDownload/{fileName}")
public ResponseEntity<Resource> fileInDirDownload(@PathVariable String fileName,
                                                  @AuthenticationPrincipal User user,HttpSession session){
    String fileAddress="";
    List<GeneralAccess> generalAccesses= generalAccessRepository.queryByUsUser(user.getId());
    for(GeneralAccess i : generalAccesses){
        if(i.getAddress().contains(fileName)){
            fileAddress=i.getAddress();
        }
    }
    String uploadDirectory = (String) session.getAttribute("uploadDirectory");
    if(fileAddress==""){
        fileAddress=uploadDirectory+"/"+fileName;
    }
    File file = new File(fileAddress);
    if (file.exists()) {
        String originalExtension = ".txt";
        if (fileName.toLowerCase().endsWith(originalExtension)) {
            Resource resource = new FileSystemResource(file);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + UriUtils.encode(fileName, StandardCharsets.UTF_8) + "\"");

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
    @GetMapping("/downloadAccessDirs/{dirName}")
    public ResponseEntity<Resource> DirDownload(@PathVariable String dirName, HttpSession session,@AuthenticationPrincipal User user) throws UnsupportedEncodingException {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        String dirAddress="";
        List<GeneralAccess> generalAccesses= generalAccessRepository.queryByUsUser(user.getId());
        for(GeneralAccess i : generalAccesses){
            if(i.getAddress().contains(dirName)){
                dirAddress=i.getAddress();
            }
        }
        if (dirAddress==""){
            dirAddress=uploadDirectory+"/"+dirName;
        }
        String zipFileName = dirName + ".zip";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            File fileSource = new File(dirAddress);
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
    @GetMapping("/dirOpenAccess/{dirName}")
    public String dirOpen(Model model, @PathVariable String dirName, @AuthenticationPrincipal User user , HttpSession session, HttpServletResponse response){
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
    try{
        int idDate= Integer.parseInt(dirName);
        List<GeneralAccess> generalAccess = generalAccessRepository.queryByUsUser(user.getId());
        for(GeneralAccess i: generalAccess){
            if(i.getId()==idDate){
                uploadDirectory = i.getAddress();
                break;
            }
        }

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
        model.addAttribute("prov",true);
        String uploadDir = uploadDirectory.replaceFirst("C:\\\\upload/", "");
        model.addAttribute("dirs",folderNames);
        model.addAttribute("files",fileNames);
        model.addAttribute("path",uploadDir);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        session.setAttribute("uploadDirectory", uploadDirectory);
        return "access-directories";

    }catch (NumberFormatException e){
        uploadDirectory=uploadDirectory+"/"+dirName;
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
        model.addAttribute("prov",true);
        String uploadDir = uploadDirectory.replaceFirst("C:\\\\upload/", "");
        model.addAttribute("dirs",folderNames);
        model.addAttribute("files",fileNames);
        model.addAttribute("path",uploadDir);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        session.setAttribute("uploadDirectory", uploadDirectory);
        return "access-directories";

    }


    }
    @GetMapping("/dirBackAccess")
    public String dirBack(Model model, @AuthenticationPrincipal User user, HttpSession session) {
        String uploadDirectory = (String) session.getAttribute("uploadDirectory");
        Long userID= 0L;
        String path="";
        List<GeneralAccess> generalAccesses= generalAccessRepository.queryByUsUser(user.getId());
        for(GeneralAccess generalAccess : generalAccesses){
            if(generalAccess.getAddress().contains(uploadDirectory)){
                userID=generalAccess.getUserId();
                path=generalAccess.getAddress();
                break;
            }
        }
        int userId = Math.toIntExact(userID);
        User users = userRepository.findById(userId);

        if(uploadDirectory.equals(path)){
            return "redirect:/filesAccess";
        }
        int lastIndex = uploadDirectory.lastIndexOf("/");
        uploadDirectory = uploadDirectory.substring(0, lastIndex);
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
        else model.addAttribute("prov",true);
        String uploadDir = uploadDirectory.replaceFirst("C:\\\\upload/", "");
        model.addAttribute("dirs", folderNames);
        model.addAttribute("files", fileNames);
        model.addAttribute("path",uploadDir);
        model.addAttribute("folderDate",folderDate);
        model.addAttribute("fileDate",fileDate);
        model.addAttribute("folderSize",folderSize);
        model.addAttribute("fileSize",fileSize);
        session.setAttribute("uploadDirectory", uploadDirectory);
        return "access-directories";
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
