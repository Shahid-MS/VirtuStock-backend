package com.virtu_stock.Controllers;

import java.security.Principal;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.virtu_stock.DTO.Request.RatingRequestDTO;
import com.virtu_stock.DTO.Response.UserResponseDTO;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;
import com.virtu_stock.Models.CustomUserDetails;
import com.virtu_stock.Models.User;
import com.virtu_stock.Service.ImageUploadService;
import com.virtu_stock.Service.RatingService;
import com.virtu_stock.Service.UserService;
import com.virtu_stock.Util.JWTUtil;

import jakarta.validation.Valid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RatingService ratingService;
    private final ModelMapper modelMapper;
    private final ImageUploadService imageUploadService;
    private final JWTUtil jwtUtil;

    @GetMapping()
    public ResponseEntity<?> userDetails(Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        UserResponseDTO userRes = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userRes);
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUser(Principal principal, @RequestPart("updates") String updatesJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        User user = userService.findByEmail(principal.getName());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> updates;
        try {
            updates = objectMapper.readValue(updatesJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON format in updates");
        }

        if (file != null && !file.isEmpty()) {
            String imageUrl = imageUploadService.uploadImage(file, "virtustock", "profile_pic",
                    user.getId().toString());
            updates.put("profilePicUrl", imageUrl);
        }
        UserResponseDTO userRes = userService.updateUser(user, updates);
        return ResponseEntity.ok(Map.of("message", "User updated Successfully", "user", userRes));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String newJwt = jwtUtil.generateToken(userDetails);
        return ResponseEntity.ok(
                Map.of("virtustock-token", newJwt));
    }

    @PostMapping("/rating")
    public ResponseEntity<?> rate(Principal principal, @RequestBody @Valid RatingRequestDTO req) {
        User user = userService.findByEmail(principal.getName());
        ratingService.rate(user, req);
        return ResponseEntity.ok(Map.of("message", "Thanks for your Feedback"));
    }

    @GetMapping("/rating")
    public ResponseEntity<?> ratedByUser(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        Integer ratingByUSer = ratingService.ratedByUser(user);
        return ResponseEntity.ok(Map.of("rating", ratingByUSer));
    }

}
