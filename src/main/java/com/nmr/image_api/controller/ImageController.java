package com.nmr.image_api.controller;

import com.nmr.image_api.dto.ImageMetaResponse;
import com.nmr.image_api.dto.UploadResponse;
import com.nmr.image_api.dto.UpdateMetaRequest;
import com.nmr.image_api.entity.ImageAsset;
import com.nmr.image_api.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final ImageService service;

    public ImageController(ImageService service) {
        this.service = service;
    }

    /** POST /images/upload — upload an image */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            @RequestParam("referenceId") String referenceId,
            @RequestParam("referenceType") String referenceType
    ) {
        ImageAsset saved = service.upload(file, uploadedBy, referenceId, referenceType);

        String downloadUrl = "/images/ref?referenceId=" + referenceId + "&referenceType=" + referenceType;
        String metaUrl     = "/images/" + saved.getId() + "/meta";

        log.info("Uploaded image id={} name={}", saved.getId(), saved.getOriginalFilename());
        return ResponseEntity.created(java.net.URI.create(downloadUrl))
                .body(new UploadResponse(saved.getId(), downloadUrl, metaUrl));
    }

    /** GET /images/ref — download by referenceId + referenceType */
    @GetMapping("/ref")
    public ResponseEntity<Resource> getByReference(
            @RequestParam String referenceId,
            @RequestParam String referenceType
    ) {
        var meta = service.getMetaByReference(referenceId, referenceType);
        Resource res = service.getByReference(referenceId, referenceType);

        String filename = (meta.originalFilename() != null && !meta.originalFilename().isBlank())
                ? meta.originalFilename()
                : referenceId + "-" + referenceType;

        return ResponseEntity.ok()
                .contentType(service.getMediaTypeByReference(referenceId, referenceType))
                .contentLength(meta.sizeBytes())
                .eTag("\"" + meta.checksumSha256() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(res);
    }

    /** GET /images/{id} — get raw image by id */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(
            @PathVariable long id,
            @RequestHeader(value = "If-None-Match", required = false) String inm
    ) {
        ImageMetaResponse meta = service.getMeta(id);
        Resource res = service.getImageData(id);

        String etag = "\"" + meta.checksumSha256() + "\"";
        if (etag.equals(inm)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        return ResponseEntity.ok()
                .contentType(service.getMediaType(id))
                .contentLength(meta.sizeBytes())
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .body(res);
    }

    /** GET /images/{id}/meta — get metadata as JSON */
    @GetMapping("/{id}/meta")
    public ImageMetaResponse getMeta(@PathVariable long id) {
        return service.getMeta(id);
    }

    /** GET /images/allmeta — list all metadata */
    @GetMapping("/allmeta")
    public List<ImageMetaResponse> listAllMeta() {
        return service.getAllMeta();
    }

    /** PUT /images/{id}/meta — update uploadedBy / filename */
    @PutMapping(path = "/{id}/meta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImageMetaResponse editMeta(@PathVariable long id, @RequestBody UpdateMetaRequest req) {
        return service.updateMeta(id, req.uploadedBy(), req.originalFilename());
    }

    /** DELETE /images/{id} — delete image + metadata */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
