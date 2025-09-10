package com.nmr.image_api.dto;

import java.time.Instant;

public record ImageMetaResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant uploadedAt,
        String checksumSha256,
        Integer width,
        Integer height
) {}
