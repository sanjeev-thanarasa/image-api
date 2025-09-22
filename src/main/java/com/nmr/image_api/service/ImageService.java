package com.nmr.image_api.service;

import com.nmr.image_api.dto.ImageMetaResponse;
import com.nmr.image_api.entity.ImageAsset;
import com.nmr.image_api.repo.ImageAssetRepository;
import com.nmr.image_api.util.HashUtil;
import com.nmr.image_api.web.BadRequestException;
import com.nmr.image_api.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final Path root;
    private final ImageAssetRepository repo;

    public ImageService(@Value("${storage.dir:./storage}") String dir, ImageAssetRepository repo) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        this.repo = repo;
        log.info("Storage at {}", root);
    }


    public ImageAsset upload(MultipartFile file, String uploadedBy, String referenceId, String referenceType) {
        if (file == null || file.isEmpty()) throw new BadRequestException("file required");
        if (referenceId == null || referenceId.isBlank()) throw new BadRequestException("referenceId required");
        if (referenceType == null || referenceType.isBlank()) throw new BadRequestException("referenceType required");
        if (uploadedBy == null || uploadedBy.isBlank()) uploadedBy = "anonymous";

        String reqType = file.getContentType() == null ? "" : file.getContentType();
        if (!reqType.startsWith("image/")) throw new BadRequestException("only image/* allowed");

        String ext = getExt(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = root.resolve(storedName).normalize();
        ensureInside(target);

        byte[] bytes;
        try {
            bytes = file.getBytes();
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("cannot write file");
        }

        // detect dimensions
        Integer w = null, h = null;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(in);
            if (img != null) { w = img.getWidth(); h = img.getHeight(); }
        } catch (IOException ignored) {}

        ImageAsset asset = new ImageAsset();
        asset.setOriginalFilename(file.getOriginalFilename());
        asset.setStoredFilename(storedName);
        asset.setContentType(reqType);
        asset.setSizeBytes(bytes.length);
        asset.setUploadedBy(uploadedBy);
        asset.setUploadedAt(Instant.now());
        asset.setChecksumSha256(HashUtil.sha256(bytes));
        asset.setWidth(w);
        asset.setHeight(h);
        asset.setReferenceId(referenceId);
        asset.setReferenceType(referenceType);

        return repo.save(asset);
    }

    public Resource getByReference(String referenceId, String referenceType) {
        ImageAsset a = repo.findTopByReferenceIdAndReferenceTypeOrderByUploadedAtDesc(referenceId, referenceType)
                .orElseThrow(() -> new NotFoundException("no image for ref"));
        Path p = root.resolve(a.getStoredFilename()).normalize();
        ensureInside(p);
        if (!Files.exists(p)) throw new NotFoundException("file missing on disk");
        return new FileSystemResource(p);
    }

    public Resource getImageData(long id) {
        ImageAsset a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        Path p = root.resolve(a.getStoredFilename()).normalize();
        ensureInside(p);
        if (!Files.exists(p)) throw new NotFoundException("file missing on disk");
        return new FileSystemResource(p);
    }

    public ImageMetaResponse getMeta(long id) {
        ImageAsset a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        return toMeta(a);
    }

    public MediaType getMediaType(long id) {
        ImageAsset a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        try {
            return a.getContentType() != null ? MediaType.parseMediaType(a.getContentType()) : MediaType.IMAGE_PNG;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public MediaType getMediaTypeByReference(String refId, String refType) {
        ImageAsset a = repo.findTopByReferenceIdAndReferenceTypeOrderByUploadedAtDesc(refId, refType)
                .orElseThrow(() -> new NotFoundException("no image for ref"));
        try {
            return a.getContentType() != null ? MediaType.parseMediaType(a.getContentType()) : MediaType.IMAGE_PNG;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public List<ImageMetaResponse> getAllMeta() {
        return repo.findAll().stream().map(this::toMeta).toList();
    }

    public ImageMetaResponse updateMeta(long id, String uploadedBy, String originalFilename) {
        var a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        if (uploadedBy != null && !uploadedBy.isBlank()) a.setUploadedBy(uploadedBy.trim());
        if (originalFilename != null && !originalFilename.isBlank()) a.setOriginalFilename(originalFilename.trim());
        return toMeta(repo.save(a));
    }

    public void deleteImage(long id) {
        var a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        var p = root.resolve(a.getStoredFilename()).normalize();
        ensureInside(p);
        try { Files.deleteIfExists(p); } catch (Exception ignore) {}
        repo.delete(a);
    }

    public ImageMetaResponse getMetaByReference(String refId, String refType) {
        var a = repo.findTopByReferenceIdAndReferenceTypeOrderByUploadedAtDesc(refId, refType)
                .orElseThrow(() -> new NotFoundException("no image for ref"));
        return toMeta(a);
    }

    private static String getExt(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }

    private void ensureInside(Path p) {
        if (!p.normalize().startsWith(root)) throw new BadRequestException("invalid path");
    }

    private ImageMetaResponse toMeta(ImageAsset a) {
        return new ImageMetaResponse(
                a.getId(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(),
                a.getUploadedBy(), a.getUploadedAt(), a.getChecksumSha256(),
                a.getWidth(), a.getHeight(), a.getReferenceId(), a.getReferenceType()
        );
    }
}
