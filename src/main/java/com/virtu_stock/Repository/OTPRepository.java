package com.virtu_stock.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.virtu_stock.Enum.OTPPurpose;
import com.virtu_stock.Models.OTP;

public interface OTPRepository extends JpaRepository<OTP, String> {

    Optional<OTP> findByEmailAndPurpose(String email, OTPPurpose purpose);

    void deleteByEmailAndPurpose(String email, OTPPurpose purpose);

    void deleteAllByCreatedAtBefore(LocalDateTime cutoff);

}
