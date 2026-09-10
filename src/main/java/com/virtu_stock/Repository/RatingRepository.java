
package com.virtu_stock.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.virtu_stock.Models.Rating;
import com.virtu_stock.Models.User;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByUser(User user);

    @Query("""
                SELECT r.rating, COUNT(r)
                FROM Rating r
                GROUP BY r.rating
            """)
    List<Object[]> getRatingDistribution();

    @Query("""
                SELECT COUNT(r), AVG(r.rating)
                FROM Rating r
            """)
    List<Object[]> getAverageRating();
}