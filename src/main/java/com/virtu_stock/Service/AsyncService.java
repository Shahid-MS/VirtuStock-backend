package com.virtu_stock.Service;

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
import com.virtu_stock.Models.IssueSize;
import com.virtu_stock.Repository.IPORepository;

import jakarta.transaction.Transactional;
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

    @Async
    @Transactional
    public void fetchAndUpdateIPOInBackground(String status, String type, Integer months, Integer limit,
            String userEmail) {
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
            List<IPO> unmatchedIpos = ipoRepository.findByIpoGrSlugIsNull();

            Map<String, IPO> existingIpoMap = existingIpos.stream()
                    .collect(Collectors.toMap(IPO::getIpoGrSlug, ipo -> ipo));

            List<IPO> newIpos = new ArrayList<>();

            for (IPOGr ipoGr : ipoGrResponse.getData()) {

                try {
                    IPO existingIpo = existingIpoMap.get(ipoGr.getSlug());

                    if (existingIpo == null) {
                        // Matched IPO but slug null
                        String firstWord = ipoGr.getName()
                                .trim()
                                .split("\\s+")[0];

                        IPO matchedIpo = unmatchedIpos.stream()
                                .filter(ipo -> ipo.getName() != null)
                                .filter(ipo -> ipo.getName()
                                        .toLowerCase()
                                        .contains(firstWord.toLowerCase()))
                                .findFirst()
                                .orElse(null);
                        if (matchedIpo == null) {
                            if (ipoGr.getName() == null || ipoGr.getName().isBlank()
                                    || ipoGr.getType() == null || ipoGr.getType().isBlank()
                                    || ipoGr.getOpenDate() == null
                                    || ipoGr.getCloseDate() == null
                                    || ipoGr.getAllotmentDate() == null
                                    || ipoGr.getListingDate() == null
                                    || ipoGr.getPriceMin() == null
                                    || ipoGr.getPriceMax() == null
                                    || ipoGr.getLotSize() == null
                                    || ipoGr.getLogo() == null
                                    || ipoGr.getLogo().isBlank()) {

                                continue;
                            }

                            String symbol = ipoGr.getName()
                                    .trim()
                                    .split("\\s+")[0]
                                    .toUpperCase();
                            IPO newIpo = new IPO();

                            newIpo.setIpoGrSlug(ipoGr.getSlug());
                            newIpo.setName(ipoGr.getName());
                            newIpo.setSymbol(symbol);
                            newIpo.setType(ipoGr.getType());

                            newIpo.setStartDate(ipoGr.getOpenDate());
                            newIpo.setEndDate(ipoGr.getCloseDate());
                            newIpo.setAllotmentDate(ipoGr.getAllotmentDate());
                            newIpo.setListingDate(ipoGr.getListingDate());

                            newIpo.setMinPrice(ipoGr.getPriceMin());
                            newIpo.setMaxPrice(ipoGr.getPriceMax());
                            newIpo.setMinQty(ipoGr.getLotSize());

                            newIpo.setLogo(ipoGr.getLogo());
                            newIpo.setIpoAlertId(null);
                            newIpo.setGmp(new ArrayList<>());

                            newIpo.setIssueSize(new IssueSize("0", "0", "0"));

                            newIpos.add(newIpo);

                            existingIpoMap.put(ipoGr.getSlug(), newIpo);

                            existingIpo = newIpo;
                        } else {
                            matchedIpo.setIpoGrSlug(ipoGr.getSlug());
                            existingIpoMap.put(ipoGr.getSlug(), matchedIpo);
                            existingIpo = matchedIpo;
                        }

                    }

                    if (existingIpo != null) {
                        // Update GMP
                        IPOGrGMP ipoGrGMP = ipoGr.getGmp();
                        if (ipoGrGMP == null
                                || ipoGrGMP.getPrice() == null
                                || ipoGrGMP.getPrice().isBlank()
                                || ipoGrGMP.getUpdatedAt() == null) {
                            continue;
                        }

                        Instant updatedAt = ipoGrGMP.getUpdatedAt();
                        LocalDate ipoGrDate = updatedAt
                                .atZone(ZoneId.of("Asia/Kolkata"))
                                .toLocalDate();
                        GMP latestGmp = existingIpo.getGmp().stream()
                                .filter(gmp -> gmp.getGmpDate().equals(ipoGrDate))
                                .findFirst()
                                .orElse(null);
                        double ipoGrPrice = Double.parseDouble(ipoGrGMP.getPrice());

                        if (latestGmp != null && latestGmp.getGmpDate().equals(ipoGrDate)) {
                            if (latestGmp.getGmp() != ipoGrPrice)
                                latestGmp.setGmp(ipoGrPrice);
                            latestGmp.setLastUpdated(
                                    updatedAt.atZone(ZoneId.of("Asia/Kolkata"))
                                            .toLocalDateTime());
                        } else {
                            GMP newGMP = new GMP();
                            newGMP.setGmp(ipoGrPrice);
                            newGMP.setGmpDate(updatedAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate());
                            newGMP.setLastUpdated(updatedAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
                            existingIpo.getGmp().add(newGMP);
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            if (!newIpos.isEmpty()) {
                ipoRepository.saveAll(newIpos);
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
