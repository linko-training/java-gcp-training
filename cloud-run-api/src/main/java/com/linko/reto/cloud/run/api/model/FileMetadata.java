package com.linko.reto.cloud.run.api.model;

public record FileMetadata(
        String name,
        String contentType,
        Long size,
        String mediaLink,
        String updated
) {}
