package com.nmr.image_api.service;

import com.nmr.image_api.dto.ImageMetaResponse;
import com.nmr.image_api.entity.ImageAsset;
import com.nmr.image_api.repo.ImageAssetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    private Path tmpDir;
    private ImageAssetRepository repo;
    private ImageService svc;

    @BeforeEach
    void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("imgsvc-test");
        repo = mock(ImageAssetRepository.class);
        svc = new ImageService(tmpDir.toString(), repo);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var s = Files.list(tmpDir)) {
            s.forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
        Files.deleteIfExists(tmpDir);
    }

    @Test
    void upload_and_getMeta_success() throws Exception {
        byte[] png = Files.readAllBytes(Path.of("target/classes/templates/images.html"));
        MockMultipartFile mf = new MockMultipartFile("file","hello.png","image/png", png);

        ImageAsset saved = new ImageAsset();
        saved.setId(1L);
        saved.setOriginalFilename("hello.png");
        saved.setStoredFilename("stored.png");
        saved.setContentType("image/png");
        saved.setSizeBytes(png.length);
        saved.setUploadedBy("me");
        saved.setUploadedAt(Instant.now());
        saved.setChecksumSha256("abc");
        saved.setReferenceId("ref1");
        saved.setReferenceType("typeA");

        when(repo.save(any())).thenReturn(saved);

        var out = svc.upload(mf, "me", "ref1", "typeA");
        assertEquals(1L, out.getId());
        verify(repo).save(any());

        when(repo.findById(1L)).thenReturn(Optional.of(saved));
        ImageMetaResponse meta = svc.getMeta(1L);
        assertEquals("hello.png", meta.originalFilename());
    }

    @Test
    void getMediaType_parsesOrDefaults() {
        ImageAsset a = new ImageAsset();
        a.setId(2L);
        a.setContentType("image/jpeg");
        when(repo.findById(2L)).thenReturn(Optional.of(a));
        assertEquals(MediaType.IMAGE_JPEG, svc.getMediaType(2L));

        a.setContentType("not/a/type");
        when(repo.findById(2L)).thenReturn(Optional.of(a));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, svc.getMediaType(2L));
    }

    @Test
    void getByReference_missingFile_throws() {
        ImageAsset a = new ImageAsset();
        a.setStoredFilename("does-not-exist.png");
        a.setReferenceId("r1"); a.setReferenceType("t");
        when(repo.findTopByReferenceIdAndReferenceTypeOrderByUploadedAtDesc("r1","t")).thenReturn(Optional.of(a));
        var ex = assertThrows(RuntimeException.class, () -> svc.getByReference("r1","t"));
        assertTrue(ex.getMessage().contains("file missing"));
    }

    @Test
    void deleteImage_deletesFileAndRepo() throws Exception {
        // create a file in tmpDir
        Path f = Files.createTempFile(tmpDir, "f", ".bin");
        byte[] b = "data".getBytes();
        Files.write(f, b);

        ImageAsset a = new ImageAsset();
        a.setId(3L);
        a.setStoredFilename(f.getFileName().toString());
        when(repo.findById(3L)).thenReturn(Optional.of(a));

        svc.deleteImage(3L);

        verify(repo).delete(a);
        assertFalse(Files.exists(f));
    }

}
