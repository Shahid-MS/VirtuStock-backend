package com.virtu_stock.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.virtu_stock.DTO.Response.IPOAlertsDTO;
import com.virtu_stock.Helper.IPOHelper;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Repository.IPORepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncService {
    private final IPORepository ipoRepository;

    private final IPOAlertsService ipoAlertsService;

    private final IPOHelper ipoHelper;

    private final MailService mailService;

    @SuppressWarnings("unchecked")
    @Async
    public void fetchIPOInBackground(String status, String type, int limit, String userEmail) {

        List<IPOAlertsDTO.Saved> saved = new ArrayList<>();
        List<IPOAlertsDTO.Skipped> skipped = new ArrayList<>();
        List<IPOAlertsDTO.Exists> exists = new ArrayList<>();
        List<IPOAlertsDTO.Error> errors = new ArrayList<>();
        try {
            Map<String, Object> firstResponse = ipoAlertsService.getIPOs(status, type, 1, limit);
            Map<String, Object> meta = (Map<String, Object>) firstResponse.get("meta");
            int totalPages = (int) meta.get("totalPages");

            for (int page = 1; page <= totalPages; page++) {
                Thread.sleep(15000);

                Map<String, Object> response = ipoAlertsService.getIPOs(status, type, page, limit);
                List<Map<String, Object>> ipos = (List<Map<String, Object>>) response.get("ipos");

                for (Map<String, Object> ipoRes : ipos) {

                    String ipoId = (String) ipoRes.get("id");
                    String ipoName = (String) ipoRes.get("name");

                    if ("DEBT".equals(ipoRes.get("type"))) {
                        skipped.add(new IPOAlertsDTO.Skipped(ipoId, ipoName, "Debt type"));
                        continue;
                    }

                    if (ipoRepository.existsByIpoAlertId(ipoId)) {
                        exists.add(new IPOAlertsDTO.Exists(ipoId, ipoName));
                        continue;
                    }

                    try {

                        IPO ipo = ipoHelper.mapToIPO(ipoRes);
                        IPO savedIpo =ipoRepository.save(ipo);
                        saved.add(new IPOAlertsDTO.Saved(savedIpo.getId(), savedIpo.getName(), savedIpo.getType()));
                    } catch (Exception e) {
                        errors.add(new IPOAlertsDTO.Error(ipoId, ipoName, e.getMessage()));
                    }
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("Total", totalPages);
            summary.put("Total Saved", saved.size());
            summary.put("Total Exists", exists.size());
            summary.put("Total Skipped", skipped.size());
            summary.put("Total Errors", errors.size());
            summary.put("Saved Ipos", saved);
            summary.put("Exists Ipos", exists);
            summary.put("Skipped Ipos", skipped);
            summary.put("Errors Ipos", errors);
            mailService.sendIpoFetchSummaryEmail(userEmail, summary);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("title", "IPO Fetch Error");
            error.put("message", e.getMessage());
            error.put("details", "Unexpected error during IPO fetch");
            mailService.sendAsyncErrorMail(userEmail, error);
        }
    }
}
