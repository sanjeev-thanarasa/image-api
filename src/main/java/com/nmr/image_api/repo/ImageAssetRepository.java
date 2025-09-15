package com.nmr.image_api.repo;

import com.nmr.image_api.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
    Optional<ImageAsset> findTopByReferenceIdAndReferenceTypeOrderByUploadedAtDesc(
            String referenceId, String referenceType);
}
