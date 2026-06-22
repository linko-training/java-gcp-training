package com.linko.reto.cloud.run.api.controller;

import com.linko.reto.cloud.run.api.model.FileMetadata;
import com.linko.reto.cloud.run.api.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileMetadata>> listFiles() {
        return ResponseEntity.ok(storageService.listFiles());
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileMetadata metadata = storageService.uploadFile(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/files/{id}")
    public ResponseEntity<FileMetadata> updateMetadata(@PathVariable("id") String id, @RequestParam("contentType") String contentType) {
        try {
            FileMetadata metadata = storageService.updateMetadata(id, contentType);
            return ResponseEntity.ok(metadata);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable("id") String id) {
        boolean deleted = storageService.deleteFile(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
