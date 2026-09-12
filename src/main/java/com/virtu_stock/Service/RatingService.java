package com.virtu_stock.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.virtu_stock.DTO.Request.RatingRequestDTO;
import com.virtu_stock.DTO.Response.RatingResponseDTO;
import com.virtu_stock.Models.Rating;
import com.virtu_stock.Models.User;
import com.virtu_stock.Repository.RatingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;

    public void rate(User user, RatingRequestDTO req) {
        Rating rating = ratingRepository.findByUser(user).orElseGet(() -> {
            Rating newRating = new Rating();
            newRating.setUser(user);
            return newRating;
        });

        rating.setRating(req.getRating());

        ratingRepository.save(rating);
    }

    public Integer ratedByUser(User user) {
        return ratingRepository.findByUser(user)
                .map(Rating::getRating)
                .orElse(0);
    }

    public RatingResponseDTO rating() {
        RatingResponseDTO ratingResponseDTO = new RatingResponseDTO();
        Object[] avgResult = ratingRepository.getAverageRating().get(0);

        Long totalUsers = (Long) avgResult[0];
        Double averageRating = (Double) avgResult[1];

        ratingResponseDTO.setAverageRating(
                averageRating != null
                        ? Math.round(averageRating * 10.0) / 10.0
                        : 0.0);
        ratingResponseDTO.setTotalUsers(totalUsers);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i > 0; i--) {
            distribution.put(i, 0L);
        }
        for (Object[] row : ratingRepository.getRatingDistribution()) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(rating, count);
        }
        ratingResponseDTO.setDistribution(distribution);

        return ratingResponseDTO;
    }

}
