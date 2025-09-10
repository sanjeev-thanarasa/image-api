package com.nmr.image_api.dto;

public record UpdateMetaRequest(
        String uploadedBy,
        String originalFilename // optional: update display name only (file on disk stays UUID)
) {}
