package com.virtu_stock.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.DTO.Response.RatingResponseDTO;
import com.virtu_stock.Service.RatingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feedback/rating")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @GetMapping()
    public ResponseEntity<RatingResponseDTO> rating() {
        return ResponseEntity.ok(ratingService.rating());
    }
}
