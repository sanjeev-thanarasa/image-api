package com.nmr.image_api.dto;

public record UploadResponse(
        Long id,
        String downloadUrl,
        String metaUrl
) {}
