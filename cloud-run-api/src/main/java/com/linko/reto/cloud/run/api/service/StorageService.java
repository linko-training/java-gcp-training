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
    private final String bucketPrefix;

    public StorageService(Storage storage, @Value("${gcp.bucket.name}") String bucketName, @Value("${gcp.bucket.prefix}") String bucketPrefix) {
        this.storage = storage;
        this.bucketName = bucketName;
        // Validar si hay prefijo y asegurar que termina con "/" para que actue como subcarpeta
        this.bucketPrefix = bucketPrefix.isEmpty() ? "" : bucketPrefix.endsWith("/") ? bucketPrefix : bucketPrefix + "/";
    }

    // 1. LISTAR ARCHIVOS (GET /files)
    public List<FileMetadata> listFiles() {
        List<FileMetadata> metadataList = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(bucketPrefix));
        for (Blob blob : blobs.iterateAll()) {
            metadataList.add(new FileMetadata(
                    blob.getName(),
                    //blob.getName().replace(bucketPrefix, ""), // Remover el prefijo del nombre del archivo
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
        //String fileName = file.getOriginalFilename();
        String fileName = bucketPrefix + file.getOriginalFilename();
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
        String fullFileName = bucketPrefix + fileName; // Asegurar que se use el prefijo al actualizar
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
    public boolean deleteFile(String fn) {
        String fileName = bucketPrefix + fn; // Asegurar que se use el prefijo al eliminar
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storage.delete(blobId);
    }

}
