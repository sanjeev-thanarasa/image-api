package com.nmr.image_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "image_asset")
public class ImageAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)  private String originalFilename;
    @Column(nullable = false, unique = true) private String storedFilename; // UUID.ext on disk
    @Column(nullable = false)  private String contentType;
    @Column(nullable = false)  private long   sizeBytes;
    @Column(nullable = false)  private String uploadedBy;
    @Column(nullable = false)  private Instant uploadedAt;

    @Column(nullable = false)  private String checksumSha256; // for ETag/caching
    private Integer width;   // optional: image width
    private Integer height;  // optional: image height

    @Column(nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private String referenceType;


}
