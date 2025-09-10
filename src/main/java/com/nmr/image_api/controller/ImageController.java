package com.nmr.image_api.controller;

import com.nmr.image_api.dto.ImageMetaResponse;
import com.nmr.image_api.dto.UploadResponse;
import com.nmr.image_api.entity.ImageAsset;
import com.nmr.image_api.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.nmr.image_api.dto.UpdateMetaRequest;

@RestController
@RequestMapping
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final ImageService service;

    public ImageController(ImageService service) { this.service = service; }

    /** POST /images/upload */
    @PostMapping(path = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy
    ){
        ImageAsset saved = service.upload(file, uploadedBy);
        String downloadUrl = "/images/" + saved.getId();
        String metaUrl     = "/images/" + saved.getId() + "/meta";
        log.info("Uploaded image id={} name={}", saved.getId(), saved.getOriginalFilename());
        return ResponseEntity.created(java.net.URI.create(downloadUrl))
                .body(new UploadResponse(saved.getId(), downloadUrl, metaUrl));
    }

    /** GET /images/{id} — returns the bytes with correct content-type */
    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable long id, @RequestHeader(value="If-None-Match",required=false) String inm){
        ImageMetaResponse meta = service.getMeta(id);
        Resource res = service.getImageData(id);

        // ETag support (use checksum)
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

    /** GET /images/{id}/meta — metadata as JSON */
    @GetMapping("/images/{id}/meta")
    public ImageMetaResponse getMeta(@PathVariable long id){
        return service.getMeta(id);
    }

    @GetMapping("/images/allmeta")
    public List<ImageMetaResponse> listAllMeta() {
        return service.getAllMeta();
    }

    @PutMapping(path = "/images/{id}/meta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImageMetaResponse editMeta(@PathVariable long id, @RequestBody UpdateMetaRequest req) {
        return service.updateMeta(id, req.uploadedBy(), req.originalFilename());
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteImage(id);
        return ResponseEntity.noContent().build();
    }


}
