package com.virtu_stock.Service;

import java.time.LocalDateTime;

import java.util.Optional;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.virtu_stock.Enum.OTPPurpose;
import com.virtu_stock.Models.OTP;
import com.virtu_stock.Repository.OTPRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OTPService {
    private final OTPRepository otpRepository;
    private final MailService mailService;
    private final Random random = new Random();

    public void generateAndSendOtp(String email, OTPPurpose purpose) {
        String otp = String.format("%06d", random.nextInt(999999));
        otpRepository.findByEmailAndPurpose(email, purpose)
                .ifPresent(existing -> otpRepository.delete(existing));
        OTP otpEntity = new OTP(email, otp, LocalDateTime.now(), purpose, false);
        otpRepository.save(otpEntity);
        switch (purpose) {
            case SIGN_UP -> mailService.sendOTPForRegistration(email, otp);
            case FORGOT_PASSWORD -> mailService.sendOTPForForgotPassword(email, otp);
            default -> throw new IllegalArgumentException("Unsupported OTP purpose: " + purpose);
        }
    }

    public boolean verifyOtp(String email, String otp, OTPPurpose purpose) {
        Optional<OTP> optionalOtp = otpRepository.findById(email);
        if (optionalOtp.isEmpty())
            return false;

        OTP otpEntity = optionalOtp.get();

        if (otpEntity.getPurpose() != purpose)
            return false;

        if (otpEntity.isVerified())
            return true;
        if (!otpEntity.getOtp().equals(otp))
            return false;
        if (otpEntity.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            otpRepository.deleteById(email);
            return false;
        }

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);
        return true;
    }

    public boolean isEmailVerified(String email, OTPPurpose purpose) {
        return otpRepository.findByEmailAndPurpose(email, purpose)
                .map(OTP::isVerified)
                .orElse(false);
    }

    @Transactional
    public void deleteByEmailAndPurpose(String email, OTPPurpose purpose) {
        otpRepository.deleteByEmailAndPurpose(email, purpose);
    }

    @Transactional
    // run every 1 hr
    @Scheduled(fixedRate = 3600000)
    public void clearExpiredOtps() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        otpRepository.deleteAllByCreatedAtBefore(cutoff);
    }

}
