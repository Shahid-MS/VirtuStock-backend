package com.virtu_stock.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.virtu_stock.Controllers.AppliedIpo;
import com.virtu_stock.Models.AllotedIpo;
import com.virtu_stock.Repository.AllotedIpoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllotedIpoService {
    private final AllotedIpoRepository allotedIpoRepository;
    private final AppliedIpoService appliedIpoService;

    public AllotedIpo createAllotment(UUID appliedIpoId) {
        AppliedIpo appliedIpo = appliedIpoService.findById(appliedIpoId);
        AllotedIpo allotedIpo = new AllotedIpo();
        allotedIpo.setAppliedIpo(appliedIpo);
        allotedIpo.setAllotedLot(1);
        allotedIpo.setSellPrice(null);
        allotedIpo.setTaxDeducted(0.0);
        return allotedIpoRepository.save(allotedIpo);
    }

    public AllotedIpo save(AllotedIpo allotedIpo) {
        return allotedIpoRepository.save(allotedIpo);
    }

    public boolean existsByAppliedIpoId(UUID appliedIpoId) {
        return allotedIpoRepository.existsByAppliedIpoId(appliedIpoId);
    }

    public AllotedIpo findById(UUID id) {
        return allotedIpoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No Alloted Ipo find with : " + id));
    }

    public List<Object[]> sumMonthlyProfit(UUID id, int year) {
        return allotedIpoRepository.sumMonthlyProfit(id, year);
    }

    @Transactional
    public void deleteAllotment(UUID appliedIpoId) {
        AppliedIpo appliedIpo = appliedIpoService.findById(appliedIpoId);
        appliedIpo.setAllotedIpo(null);
        allotedIpoRepository.deleteByAppliedIpo(appliedIpo);
    }
}
