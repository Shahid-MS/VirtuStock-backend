package com.virtu_stock.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/jpg");

    public String uploadImage(
            MultipartFile profilePic,
            String mainFolder,
            String subFolder,
            String userId) {
        validateFile(profilePic);
        String folderPath = mainFolder + "/" + subFolder;
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    profilePic.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderPath,
                            "public_id", userId,
                            "overwrite", true,
                            "quality", "auto",
                            "resource_type", "image"));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image not present");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPG, JPEG, and PNG images are allowed");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Max file size is 2MB");
        }
    }
}
