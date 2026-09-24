package com.virtu_stock.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.virtu_stock.DTO.Response.IPOAlertsDTO;
import com.virtu_stock.DTO.Response.IPOGrResponseDTO;
import com.virtu_stock.Helper.IPOHelper;
import com.virtu_stock.Models.GMP;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.IPOGr;
import com.virtu_stock.Models.IPOGrGMP;
import com.virtu_stock.Repository.IPORepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncService {
    private final IPORepository ipoRepository;

    private final IPOAlertsService ipoAlertsService;

    private final IPOGrService ipoGrService;

    private final IPOHelper ipoHelper;

    private final MailService mailService;

    @Value("${mail.admin}")
    private String adminMail;

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
                        IPO savedIpo = ipoRepository.save(ipo);
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

    @SuppressWarnings("unchecked")
    @Async
    public void fetchAndUpdateIPOInBackground(String status, String type, Integer months, Integer limit, String userEmail) {
        try {
            IPOGrResponseDTO ipoGrResponse = ipoGrService.getIPOs(status, type, months, limit);

            if (ipoGrResponse == null || ipoGrResponse.getData() == null)
                return;

            List<String> slugs = ipoGrResponse.getData()
                    .stream()
                    .map(IPOGr::getSlug)
                    .filter(Objects::nonNull)
                    .filter(slug -> !slug.isBlank())
                    .toList();

            List<IPO> existingIpos = ipoRepository.findByIpoGrSlugIn(slugs);

            Map<String, IPO> existingIpoMap = existingIpos.stream()
                    .collect(Collectors.toMap(IPO::getIpoGrSlug, ipo -> ipo));

            for (IPOGr ipoGr : ipoGrResponse.getData()) {
                try {
                    IPO existingIpo = existingIpoMap.get(ipoGr.getSlug());

                    if (existingIpo != null) {
                        // Update GMP
                        IPOGrGMP ipoGrGMP = ipoGr.getGmp();
                        if (ipoGrGMP == null
                                || ipoGrGMP.getPrice() == null
                                || ipoGrGMP.getPrice().isBlank()
                                || ipoGrGMP.getUpdatedAt() == null) {
                            continue;
                        }

                        GMP latestGmp = existingIpo.getGmp().isEmpty()
                                ? null
                                : existingIpo.getGmp().get(0);
                        Instant updatedAt = ipoGrGMP.getUpdatedAt();
                        LocalDate ipoGrDate = updatedAt
                                .atZone(ZoneId.of("Asia/Kolkata"))
                                .toLocalDate();
                        double ipoGrPrice = Double.parseDouble(ipoGrGMP.getPrice());

                        if (latestGmp != null && latestGmp.getGmpDate().equals(ipoGrDate)
                                && latestGmp.getGmp() == ipoGrPrice) {
                            latestGmp.setLastUpdated(
                                    updatedAt.atZone(ZoneId.of("Asia/Kolkata"))
                                            .toLocalDateTime());
                        } else {
                            GMP newGMP = new GMP();
                            newGMP.setGmp(Double.parseDouble(ipoGrGMP.getPrice()));
                            newGMP.setGmpDate(updatedAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate());
                            newGMP.setLastUpdated(updatedAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
                            existingIpo.getGmp().add(newGMP);
                        }
                    }
                    else{
                        
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            // for (int page = 1; page <= totalPages; page++) {

            // Thread.sleep(15000);

            // Map<String, Object> response = ipoAlertsService.getIPOs(status, type, page,
            // limit);
            // List<Map<String, Object>> ipos = (List<Map<String, Object>>)
            // response.get("ipos");

            // for (Map<String, Object> ipoRes : ipos) {

            // String ipoId = (String) ipoRes.get("id");
            // String ipoName = (String) ipoRes.get("name");

            // if ("DEBT".equals(ipoRes.get("type"))) {
            // skipped.add(new IPOAlertsDTO.Skipped(ipoId, ipoName, "Debt type"));
            // continue;
            // }

            // if (ipoRepository.existsByIpoAlertId(ipoId)) {
            // exists.add(new IPOAlertsDTO.Exists(ipoId, ipoName));
            // continue;
            // }

            // try {

            // IPO ipo = ipoHelper.mapToIPO(ipoRes);
            // IPO savedIpo = ipoRepository.save(ipo);
            // saved.add(new IPOAlertsDTO.Saved(savedIpo.getId(), savedIpo.getName(),
            // savedIpo.getType()));

            // } catch (Exception e) {

            // errors.add(new IPOAlertsDTO.Error(ipoId, ipoName, e.getMessage()));
            // }
            // }
            // }

            // Map<String, Object> summary = new LinkedHashMap<>();
            // summary.put("Total", totalPages);
            // summary.put("Total Saved", saved.size());
            // summary.put("Total Exists", exists.size());
            // summary.put("Total Skipped", skipped.size());
            // summary.put("Total Errors", errors.size());
            // summary.put("Saved Ipos", saved);
            // summary.put("Exists Ipos", exists);
            // summary.put("Skipped Ipos", skipped);
            // summary.put("Errors Ipos", errors);

            // mailService.sendIpoFetchSummaryEmail(userEmail, summary);

        } catch (

        Exception e) {

            // Map<String, Object> error = new HashMap<>();
            // error.put("title", "IPO Fetch Error");
            // error.put("message", e.getMessage());
            // error.put("details", "Unexpected error during IPO fetch");

            // mailService.sendAsyncErrorMail(userEmail, error);

        }
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledIPOFetch() {
        fetchIPOInBackground("open", null, 1, adminMail);
    }
}
