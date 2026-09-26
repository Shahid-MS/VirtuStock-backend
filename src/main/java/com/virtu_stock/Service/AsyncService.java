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
import com.virtu_stock.DTO.Response.IPOGrSubscriptionResponseDTO;
import com.virtu_stock.DTO.Response.IPOGrSubscriptionSummary;
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
    @Transactional
    public void fetchIPOInBackground(String status, String type, int limit, String userEmail) {

        List<IPOAlertsDTO.Saved> saved = new ArrayList<>();
        List<IPOAlertsDTO.Skipped> skipped = new ArrayList<>();
        List<IPOAlertsDTO.Exists> exists = new ArrayList<>();
        List<IPOAlertsDTO.Error> errors = new ArrayList<>();
        try {

            Map<String, Object> firstResponse = ipoAlertsService.getIPOs(status, type, 1, limit);

            Map<String, Object> meta = (Map<String, Object>) firstResponse.get("meta");
            int totalPages = (int) meta.get("totalPages");

            List<IPO> unmatchedIpos = ipoRepository.findByIpoAlertIdIsNull();

            List<IPO> newIpos = new ArrayList<>();
            List<IPO> updateIpos = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {

                Thread.sleep(15000);

                Map<String, Object> response = ipoAlertsService.getIPOs(status, type, page, limit);
                List<Map<String, Object>> ipos = (List<Map<String, Object>>) response.get("ipos");

                List<String> ipoAlertIds = ipos.stream()
                        .map(ipo -> (String) ipo.get("id"))
                        .filter(Objects::nonNull)
                        .toList();

                List<IPO> existingIpos = ipoRepository.findByIpoAlertIdIn(ipoAlertIds);

                Map<String, IPO> existingIpoMap = existingIpos.stream()
                        .collect(Collectors.toMap(
                                IPO::getIpoAlertId,
                                ipo -> ipo));

                for (Map<String, Object> ipoRes : ipos) {
                    String ipoId = (String) ipoRes.get("id");
                    String ipoName = (String) ipoRes.get("name");
                    try {
                        if ("DEBT".equals(ipoRes.get("type"))) {
                            skipped.add(new IPOAlertsDTO.Skipped(ipoId, ipoName, "Debt type"));
                            continue;
                        }

                        IPO existingIpo = existingIpoMap.get(ipoId);

                        if (existingIpo == null) {
                            IPO ipoAlertsIpo = ipoHelper.mapToIPO(ipoRes);

                            String firstWord = ipoName
                                    .trim()
                                    .split("\\s+")[0];

                            IPO matchedIpo = unmatchedIpos.stream()
                                    .filter(ipo -> ipo.getName() != null)
                                    .filter(ipo -> ipo.getName()
                                            .toLowerCase()
                                            .contains(firstWord.toLowerCase()))
                                    .findFirst()
                                    .orElse(null);

                            if (matchedIpo != null) {
                                matchedIpo.setIpoAlertId(ipoId);
                                matchedIpo.setName(ipoAlertsIpo.getName());
                                matchedIpo.setSymbol(ipoAlertsIpo.getSymbol());
                                matchedIpo.setType(ipoAlertsIpo.getType());

                                matchedIpo.setStartDate(ipoAlertsIpo.getStartDate());
                                matchedIpo.setEndDate(ipoAlertsIpo.getEndDate());
                                matchedIpo.setAllotmentDate(ipoAlertsIpo.getAllotmentDate());
                                matchedIpo.setListingDate(ipoAlertsIpo.getListingDate());

                                matchedIpo.setMinPrice(ipoAlertsIpo.getMinPrice());
                                matchedIpo.setMaxPrice(ipoAlertsIpo.getMaxPrice());
                                matchedIpo.setListedPrice(ipoAlertsIpo.getListedPrice());
                                matchedIpo.setMinQty(ipoAlertsIpo.getMinQty());

                                matchedIpo.setLogo(ipoAlertsIpo.getLogo());
                                matchedIpo.setIssueSize(ipoAlertsIpo.getIssueSize());

                                matchedIpo.setAbout(ipoAlertsIpo.getAbout());
                                matchedIpo.setStrengths(ipoAlertsIpo.getStrengths());
                                matchedIpo.setRisks(ipoAlertsIpo.getRisks());

                                updateIpos.add(matchedIpo);
                                unmatchedIpos.remove(matchedIpo);
                                exists.add(
                                        new IPOAlertsDTO.Exists(
                                                ipoId,
                                                ipoName));
                            } else {
                                newIpos.add(ipoAlertsIpo);
                                saved.add(new IPOAlertsDTO.Saved(ipoAlertsIpo.getId(), ipoAlertsIpo.getName(),
                                        ipoAlertsIpo.getType()));
                            }
                        }
                    }

                    catch (Exception e) {
                        errors.add(new IPOAlertsDTO.Error(ipoId, ipoName, e.getMessage()));
                    }
                }
            }

            updateIpos.addAll(newIpos);

            if (!updateIpos.isEmpty()) {
                ipoRepository.saveAll(updateIpos);
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
            List<IPO> updateIpos = new ArrayList<>();

            for (IPOGr ipoGr : ipoGrResponse.getData()) {
                if (ipoGr.getSlug() == null || ipoGr.getSlug().isBlank()) {
                    continue;
                }

                if (ipoGr.getName() == null || ipoGr.getName().isBlank()) {
                    continue;
                }
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
                            if (ipoGr.getType() == null || ipoGr.getType().isBlank()
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
                            unmatchedIpos.remove(matchedIpo);
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

                        if (existingIpo.getGmp() == null) {
                            existingIpo.setGmp(new ArrayList<>());
                        }
                        GMP latestGmp = existingIpo.getGmp().stream()
                                .filter(gmp -> ipoGrDate.equals(gmp.getGmpDate()))
                                .findFirst()
                                .orElse(null);
                        double ipoGrPrice = Double.parseDouble(ipoGrGMP.getPrice());

                        if (latestGmp != null) {
                            if (latestGmp.getGmp() != ipoGrPrice)
                                latestGmp.setGmp(ipoGrPrice);

                            latestGmp.setLastUpdated(
                                    updatedAt.atZone(ZoneId.of("Asia/Kolkata"))
                                            .toLocalDateTime());
                        } else {
                            GMP newGMP = new GMP();
                            newGMP.setGmp(ipoGrPrice);
                            newGMP.setGmpDate(ipoGrDate);
                            newGMP.setLastUpdated(updatedAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
                            existingIpo.getGmp().add(newGMP);
                        }

                        // Listing Price Update
                        if (Boolean.TRUE.equals(ipoGr.getIsListed())
                                && ipoGr.getListingPrice() != null
                                && !ipoGr.getListingPrice().isBlank()) {

                            double listingPrice = Double.parseDouble(ipoGr.getListingPrice());

                            if (!Double.valueOf(listingPrice).equals(existingIpo.getListedPrice())) {
                                existingIpo.setListedPrice(listingPrice);
                            }
                        }

                        updateIpos.add(existingIpo);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            updateIpos.addAll(newIpos);
            if (!updateIpos.isEmpty()) {
                ipoRepository.saveAll(updateIpos);
                fetchAndSubscriptionIPOInBackground(updateIpos);
            }

        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void fetchAndSubscriptionIPOInBackground(List<IPO> ipos) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        List<IPO> openIpos = ipos.stream()
                .filter(i -> i.getIpoGrSlug() != null
                        && !i.getIpoGrSlug().isBlank())
                .filter(i -> i.getStartDate() != null
                        && i.getEndDate() != null)
                .filter(i -> !today.isBefore(i.getStartDate())
                        && !today.isAfter(i.getEndDate()))
                .toList();

        if (openIpos.isEmpty()) {
            return;
        }

        List<IPO> updateIpos = new ArrayList<>();

        for (IPO currentIpo : openIpos) {

            try {
                IPOGrSubscriptionResponseDTO response = ipoGrService.getSubscription(currentIpo.getIpoGrSlug());

                if (response == null
                        || response.getData() == null
                        || response.getData().getSummary() == null) {
                    continue;
                }

                IPOGrSubscriptionSummary summary = response.getData().getSummary();

                Map<String, Double> subscriptions = new LinkedHashMap<>();

                ipoHelper.addSubscription(subscriptions, "QIB", summary.getQib());
                ipoHelper.addSubscription(subscriptions, "Non-Institutional", summary.getNii());
                ipoHelper.addSubscription(subscriptions, "Retailer", summary.getRetail());
                ipoHelper.addSubscription(subscriptions, "Employee", summary.getEmployee());
                ipoHelper.addSubscription(subscriptions, "Shareholder", summary.getShareholder());
                ipoHelper.addSubscription(subscriptions, "Anchor", summary.getAnchor());
                ipoHelper.addSubscription(subscriptions, "Total", summary.getTotal());

                currentIpo.setSubscriptions(subscriptions);
                Instant updatedAt = summary.getUpdatedAt();
                currentIpo.setSubscriptionLastUpdated(
                        updatedAt.atZone(ZoneId.of("Asia/Kolkata"))
                                .toLocalDateTime());

                updateIpos.add(currentIpo);

            } catch (Exception e) {
                System.out.println(
                        "Subscription fetch failed for IPO: "
                                + currentIpo.getName());
                e.printStackTrace();
            }

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!updateIpos.isEmpty())
            ipoRepository.saveAll(updateIpos);
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledIPOFetch() {
        fetchIPOInBackground("open", null, 1, adminMail);
    }
}
