package com.linko.reto.cloud.run.api.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.linko.reto.cloud.run.api.model.FileMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class StorageService {

    private final Storage storage;
    private final String bucketName;

    public StorageService(Storage storage, @Value("${gcp.bucket.name}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    // 1. LISTAR ARCHIVOS (GET /files)
    public List<FileMetadata> listFiles() {
        List<FileMetadata> metadataList = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            metadataList.add(new FileMetadata(
                    blob.getName(),
                    blob.getContentType(),
                    blob.getSize(),
                    blob.getMediaLink(),
                    blob.getUpdateTimeOffsetDateTime().toString()
                    //blob.getUpdateTime() != null ? blob.getUpdateTime().toString() : null
            ));
        }
        return metadataList;
    }

    // 2. SUBIR ARCHIVO (POST /upload)
    public FileMetadata uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        // .getInputStream() maneja streaming directamente para evitar sobrecargar la memoria RAM de Cloud Run
        Blob blob = storage.create(blobInfo, file.getInputStream());

        return new FileMetadata(
                blob.getName(),
                blob.getContentType(),
                blob.getSize(),
                blob.getMediaLink(),
                blob.getUpdateTimeOffsetDateTime().toString()
        );
    }

    // 3. ACTUALIZAR METADATOS (PUT /files/{id}) - GCP asume el nombre de archivo como Id
    public FileMetadata updateMetadata(String fileName, String contentType) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);

        if (blob == null) {
            throw new RuntimeException("Archivo no encontrado en el bucket: " + fileName);
        }

        Blob updatedBlob = blob.toBuilder().setContentType(contentType).build().update();

        return new FileMetadata(
                updatedBlob.getName(),
                updatedBlob.getContentType(),
                updatedBlob.getSize(),
                updatedBlob.getMediaLink(),
                updatedBlob.getUpdateTimeOffsetDateTime().toString()
        );
    }

    // 4. ELIMINAR ARCHIVO (DELETE /files/{id})
    public boolean deleteFile(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storage.delete(blobId);
    }

}
