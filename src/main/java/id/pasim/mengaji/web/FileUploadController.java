package id.pasim.mengaji.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "mov", "m4v", "ogg");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024; // 50 MB

    private final Path uploadRoot;

    public FileUploadController(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Gagal membuat direktori upload: " + this.uploadRoot, ex);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "kind", required = false, defaultValue = "any") String kind,
            HttpServletRequest req
    ) {
        CurrentUser.get(req);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tidak boleh kosong");
        }

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String ext = extractExtension(originalName);
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();

        boolean isImage = IMAGE_EXTS.contains(ext) || contentType.startsWith("image/");
        boolean isVideo = VIDEO_EXTS.contains(ext) || contentType.startsWith("video/");

        if ("image".equalsIgnoreCase(kind) && !isImage) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format gambar harus JPG, PNG, WEBP, atau GIF");
        }
        if ("video".equalsIgnoreCase(kind) && !isVideo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format video harus MP4, WEBM, MOV, M4V, atau OGG");
        }
        if (!isImage && !isVideo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hanya file video (MP4/WEBM/MOV) atau gambar (JPG/PNG/WEBP/GIF) yang diperbolehkan");
        }

        if (isImage && file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ukuran gambar maksimal 10 MB");
        }
        if (isVideo && file.getSize() > MAX_VIDEO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ukuran video maksimal 50 MB");
        }

        if (ext.isEmpty()) {
            ext = isVideo ? "mp4" : "jpg";
        }
        String storedName = UUID.randomUUID() + "." + ext;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama file tidak valid");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan file yang diunggah");
        }

        return Map.of(
                "url", "/api/uploads/" + storedName,
                "filename", storedName,
                "originalName", originalName,
                "kind", isVideo ? "video" : "image",
                "size", file.getSize()
        );
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        if (filename == null || !filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama file tidak valid");
        }
        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File tidak ditemukan");
        }
        try {
            Resource resource = new UrlResource(target.toUri());
            String detected = Files.probeContentType(target);
            MediaType mediaType = (detected != null) ? MediaType.parseMediaType(detected) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)))
                    .body(resource);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal membaca file");
        }
    }

    private String extractExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
