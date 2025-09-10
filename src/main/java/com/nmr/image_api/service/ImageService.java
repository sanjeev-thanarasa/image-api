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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    /** store to disk + save metadata */
    public ImageAsset upload(MultipartFile file, String uploadedBy) {
        if (file == null || file.isEmpty()) throw new BadRequestException("file required");
        if (uploadedBy == null || uploadedBy.isBlank()) uploadedBy = "anonymous";

        // basic type check
        String reqType = file.getContentType() == null ? "" : file.getContentType();
        if (!reqType.startsWith("image/")) throw new BadRequestException("only image/* allowed");

        // generate server-side name
        String ext = getExt(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty()? "" : "." + ext);
        Path target = root.resolve(storedName).normalize();
        ensureInside(target);

        byte[] bytes;
        try {
            bytes = file.getBytes();
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) { throw new BadRequestException("cannot write file"); }

        // detect dimensions
        Integer w=null,h=null;
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

        return repo.save(asset);
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
        return new ImageMetaResponse(
                a.getId(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(),
                a.getUploadedBy(), a.getUploadedAt(), a.getChecksumSha256(), a.getWidth(), a.getHeight()
        );
    }

    public MediaType getMediaType(long id){
        ImageAsset a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        try {
            return a.getContentType() != null ? MediaType.parseMediaType(a.getContentType()) : MediaType.IMAGE_PNG;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String getExt(String name){
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot+1);
    }

    /** prevent path traversal */
    private void ensureInside(Path p){
        if (!p.normalize().startsWith(root)) throw new BadRequestException("invalid path");
    }

    public List<ImageMetaResponse> getAllMeta() {
        return repo.findAll().stream()
                .map(a -> new ImageMetaResponse(
                        a.getId(),
                        a.getOriginalFilename(),
                        a.getContentType(),
                        a.getSizeBytes(),
                        a.getUploadedBy(),
                        a.getUploadedAt(),
                        a.getChecksumSha256(),
                        a.getWidth(),
                        a.getHeight()
                ))
                .toList();
    }

    public ImageMetaResponse updateMeta(long id, String uploadedBy, String originalFilename) {
        var a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        if (uploadedBy != null && !uploadedBy.isBlank()) a.setUploadedBy(uploadedBy.trim());
        if (originalFilename != null && !originalFilename.isBlank()) a.setOriginalFilename(originalFilename.trim());
        a = repo.save(a);
        return new ImageMetaResponse(
                a.getId(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(),
                a.getUploadedBy(), a.getUploadedAt(), a.getChecksumSha256(), a.getWidth(), a.getHeight()
        );
    }

    public void deleteImage(long id) {
        var a = repo.findById(id).orElseThrow(() -> new NotFoundException("image not found"));
        var p = root.resolve(a.getStoredFilename()).normalize();
        ensureInside(p);
        try { Files.deleteIfExists(p); } catch (Exception ignore) {}
        repo.delete(a);
    }

}
