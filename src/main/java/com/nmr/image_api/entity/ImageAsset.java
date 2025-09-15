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

//    /* getters & setters */
//    public Long getId() { return id; }
//    public String getOriginalFilename() { return originalFilename; }
//    public void setOriginalFilename(String v) { this.originalFilename = v; }
//    public String getStoredFilename() { return storedFilename; }
//    public void setStoredFilename(String v) { this.storedFilename = v; }
//    public String getContentType() { return contentType; }
//    public void setContentType(String v) { this.contentType = v; }
//    public long getSizeBytes() { return sizeBytes; }
//    public void setSizeBytes(long v) { this.sizeBytes = v; }
//    public String getUploadedBy() { return uploadedBy; }
//    public void setUploadedBy(String v) { this.uploadedBy = v; }
//    public Instant getUploadedAt() { return uploadedAt; }
//    public void setUploadedAt(Instant v) { this.uploadedAt = v; }
//    public String getChecksumSha256() { return checksumSha256; }
//    public void setChecksumSha256(String v) { this.checksumSha256 = v; }
//    public Integer getWidth() { return width; }
//    public void setWidth(Integer width) { this.width = width; }
//    public Integer getHeight() { return height; }
//    public void setHeight(Integer height) { this.height = height; }


}
