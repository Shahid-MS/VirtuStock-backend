package com.virtu_stock.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.virtu_stock.DTO.Response.IPOAlertsDTO;
import com.virtu_stock.DTO.Response.IPOGrDTO;
import com.virtu_stock.DTO.Response.IPOGrFetchResult;
import com.virtu_stock.DTO.Response.IPOGrResponseDTO;
import com.virtu_stock.DTO.Response.IPOGrSubscriptionResponseDTO;
import com.virtu_stock.DTO.Response.IPOGrSubscriptionSummary;
import com.virtu_stock.Helper.IPOHelper;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.IPOGr;
import com.virtu_stock.Models.IPOGrGMP;
import com.virtu_stock.Models.IssueSize;
import com.virtu_stock.Projection.LatestGMPProjection;
import com.virtu_stock.Repository.GMPBatchRepository;
import com.virtu_stock.Repository.GMPInsert;
import com.virtu_stock.Repository.GMPUpdate;
import com.virtu_stock.Repository.IPOProfileRepository;
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

    private final GMPBatchRepository gmpBatchRepository;

    private final IPOProfileRepository ipoProfileRepository;

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

            List<IPO> iposAlertsNullIpos = ipoRepository.findByIpoAlertIdIsNull();

            Set<IPO> updateIpos = new HashSet<>();

            for (int page = 1; page <= totalPages; page++) {

                if (page > 1) {
                    Thread.sleep(15000);
                }

                Map<String, Object> response = page == 1
                        ? firstResponse
                        : ipoAlertsService.getIPOs(status, type, page, limit);

                List<Map<String, Object>> ipos = (List<Map<String, Object>>) response.get("ipos");

                List<String> ipoAlertIds = ipos.stream()
                        .map(ipo -> (String) ipo.get("id"))
                        .filter(Objects::nonNull)
                        .toList();

                List<IPO> existingIposByIPOAlertsId = ipoRepository.findByIpoAlertIdIn(ipoAlertIds);

                Map<String, IPO> existingIpoMapByIPOAlertsId = existingIposByIPOAlertsId.stream()
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

                        IPO existingIpo = existingIpoMapByIPOAlertsId.get(ipoId);

                        // Not Present as per IPO Alerts
                        if (existingIpo == null) {
                            IPO ipoAlertsIpo = ipoHelper.mapToIPO(ipoRes);

                            List<IPO> matchedIpos = iposAlertsNullIpos.stream()
                                    .filter(ipo -> ipoHelper.isMatchingIPO(ipo, ipoAlertsIpo))
                                    .toList();

                            IPO matchedIpo = matchedIpos.size() == 1
                                    ? matchedIpos.get(0)
                                    : null;

                            // Already Present Update
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
                                iposAlertsNullIpos.remove(matchedIpo);
                                exists.add(
                                        new IPOAlertsDTO.Exists(
                                                ipoId,
                                                ipoName + " (Updated as per IPO Alerts)"));
                            }
                            // Not Present add
                            else {
                                updateIpos.add(ipoAlertsIpo);
                                saved.add(new IPOAlertsDTO.Saved(ipoAlertsIpo.getId(), ipoAlertsIpo.getName(),
                                        ipoAlertsIpo.getType()));
                            }
                        }
                        // Already Present
                        else {
                            exists.add(
                                    new IPOAlertsDTO.Exists(
                                            ipoId,
                                            ipoName + " (Existed as per IPO Alerts)"));
                        }
                    } catch (Exception e) {
                        errors.add(new IPOAlertsDTO.Error(ipoId, ipoName, e.getMessage()));
                    }
                }
            }

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
    public void fetchAndUpdateIPOInBackground(
            String status,
            String type,
            Integer months,
            Integer limit,
            String userEmail) {

        try {
            IPOGrFetchResult ipoGrFetchResult = new IPOGrFetchResult();

            IPOGrResponseDTO ipoGrResponse = ipoGrService.getIPOs(status, type, months, limit);

            if (ipoGrResponse == null || ipoGrResponse.getData() == null) {
                ipoGrFetchResult.getErrors().add(new IPOGrDTO.Error(
                        "SME or Mainboard",
                        "IPO Gr Data Empty",
                        "Slug is Null or Blank"));

                summaryMail(ipoGrFetchResult, userEmail);
                return;
            }

            List<IPOGr> ipoGrData = ipoGrResponse.getData();

            Set<String> slugs = ipoGrData.stream()
                    .map(IPOGr::getSlug)
                    .filter(Objects::nonNull)
                    .filter(slug -> !slug.isBlank())
                    .collect(Collectors.toSet());

            addOrUpdateIPOs(
                    ipoGrData,
                    slugs,
                    ipoGrFetchResult);

            List<IPO> existingIpos = slugs.isEmpty()
                    ? Collections.emptyList()
                    : ipoRepository.findByIpoGrSlugIn(slugs);

            Map<String, IPO> slugIpoMap = existingIpos.stream()
                    .collect(Collectors.toMap(
                            IPO::getIpoGrSlug,
                            ipo -> ipo));

            updateGmp(ipoGrData, slugIpoMap, ipoGrFetchResult);

            updateListing(ipoGrData, slugIpoMap, ipoGrFetchResult);

            summaryMail(ipoGrFetchResult, userEmail);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void addOrUpdateIPOs(
            List<IPOGr> ipoGrs,
            Set<String> slugs,
            IPOGrFetchResult ipoGrFetchResult) {

        Set<IPO> updateIpos = new HashSet<>();

        List<IPO> existingIposBySlugs = slugs.isEmpty()
                ? Collections.emptyList()
                : ipoRepository.findByIpoGrSlugIn(slugs);

        List<IPO> existingIposBySlugNull = ipoRepository.findByIpoGrSlugIsNull();

        Map<String, IPO> existingIpoMapBySlugs = existingIposBySlugs.stream()
                .collect(Collectors.toMap(
                        IPO::getIpoGrSlug,
                        ipo -> ipo));

        for (IPOGr ipoGr : ipoGrs) {
            try {
                String slug = ipoGr.getSlug();
                String name = ipoGr.getName();

                if (slug == null || slug.isBlank()) {
                    ipoGrFetchResult.getErrors().add(new IPOGrDTO.Error(
                            name,
                            name,
                            "Slug is Null or Blank"));
                    continue;
                }

                if (name == null || name.isBlank()) {
                    ipoGrFetchResult.getErrors().add(new IPOGrDTO.Error(
                            name,
                            name,
                            "Name is Null or Blank"));
                    continue;
                }

                IPO existingIpo = existingIpoMapBySlugs.get(slug);

                if (existingIpo == null) {
                    if (ipoGr.getType() == null
                            || ipoGr.getType().isBlank()
                            || ipoGr.getOpenDate() == null
                            || ipoGr.getCloseDate() == null
                            || ipoGr.getAllotmentDate() == null
                            || ipoGr.getListingDate() == null
                            || ipoGr.getPriceMin() == null
                            || ipoGr.getPriceMax() == null
                            || ipoGr.getLotSize() == null
                            || ipoGr.getLogo() == null
                            || ipoGr.getLogo().isBlank()
                            || ipoGr.getIssueSize() == null
                            || ipoGr.getIssueSize().isBlank()) {

                        ipoGrFetchResult.getErrors().add(new IPOGrDTO.Error(
                                name,
                                name,
                                "New IPO Creation Data Missing"));
                        continue;
                    }

                    IPO ipoGrIpo = new IPO();

                    ipoGrIpo.setName(ipoGr.getName());
                    ipoGrIpo.setType(
                            "mainboard".equalsIgnoreCase(ipoGr.getType())
                                    ? "EQ"
                                    : ipoGr.getType());
                    ipoGrIpo.setStartDate(ipoGr.getOpenDate());
                    ipoGrIpo.setEndDate(ipoGr.getCloseDate());
                    ipoGrIpo.setMinPrice(ipoGr.getPriceMin());
                    ipoGrIpo.setMaxPrice(ipoGr.getPriceMax());

                    List<IPO> matchedIpos = existingIposBySlugNull.stream()
                            .filter(ipo -> ipoHelper.isMatchingIPO(ipo, ipoGrIpo))
                            .toList();

                    IPO matchedIpo = matchedIpos.size() == 1
                            ? matchedIpos.get(0)
                            : null;

                    if (matchedIpo != null) {

                        matchedIpo.setIpoGrSlug(slug);

                        existingIpoMapBySlugs.put(slug, matchedIpo);
                        existingIposBySlugNull.remove(matchedIpo);
                        updateIpos.add(matchedIpo);

                        ipoGrFetchResult.getExists().add(new IPOGrDTO.Exists(
                                matchedIpo.getId().toString(),
                                matchedIpo.getName() + " (slug updated)"));
                    }

                    else {
                        String symbol = name
                                .trim()
                                .split("\\s+")[0]
                                .toUpperCase();
                        ipoGrIpo.setIpoGrSlug(slug);
                        ipoGrIpo.setSymbol(symbol);
                        ipoGrIpo.setAllotmentDate(ipoGr.getAllotmentDate());
                        ipoGrIpo.setListingDate(ipoGr.getListingDate());
                        ipoGrIpo.setMinQty(ipoGr.getLotSize());
                        ipoGrIpo.setLogo(ipoGr.getLogo());
                        ipoGrIpo.setIpoAlertId(null);
                        ipoGrIpo.setGmp(new ArrayList<>());

                        ipoGrIpo.setIssueSize(
                                new IssueSize(
                                        null,
                                        null,
                                        ipoGr.getIssueSize() + "cr"));

                        existingIpoMapBySlugs.put(slug, ipoGrIpo);
                        updateIpos.add(ipoGrIpo);
                        ipoGrFetchResult.getSaved().add(new IPOGrDTO.Saved(
                                ipoGrIpo.getId(),
                                ipoGrIpo.getName(),
                                ipoGrIpo.getType()));

                    }
                }
            } catch (Exception e) {
                ipoGrFetchResult.getErrors().add(new IPOGrDTO.Error(
                        ipoGr.getSlug(),
                        ipoGr.getName(),
                        "Exception: " + e.getMessage()));

            }
        }
        if (!updateIpos.isEmpty()) {
            ipoRepository.saveAll(updateIpos);
        }
    }

    private void updateGmp(
            List<IPOGr> data,
            Map<String, IPO> slugIpoMap, IPOGrFetchResult ipoGrFetchResult) {

        if (slugIpoMap.isEmpty()) {
            return;
        }

        List<UUID> ipoIds = slugIpoMap.values().stream()
                .map(IPO::getId)
                .toList();

        List<LatestGMPProjection> latestGmpList = ipoProfileRepository.findLatestGmp(ipoIds);

        Map<UUID, LatestGMPProjection> latestGmpMap = latestGmpList.stream()
                .collect(Collectors.toMap(
                        gmp -> UUID.fromString(gmp.getIpoId()),
                        gmp -> gmp));

        List<GMPInsert> gmpInserts = new ArrayList<>();
        List<GMPUpdate> gmpUpdates = new ArrayList<>();

        ZoneId zoneId = ZoneId.of("Asia/Kolkata");

        for (IPOGr ipoGr : data) {
            try {
                IPO ipo = slugIpoMap.get(ipoGr.getSlug());

                if (ipo == null) {
                    continue;
                }

                IPOGrGMP ipoGrGmp = ipoGr.getGmp();

                if (ipoGrGmp == null
                        || ipoGrGmp.getPrice() == null
                        || ipoGrGmp.getPrice().isBlank()
                        || ipoGrGmp.getUpdatedAt() == null) {
                    continue;
                }

                double gmpPrice;

                try {
                    gmpPrice = Double.parseDouble(ipoGrGmp.getPrice());
                } catch (NumberFormatException e) {
                    ipoGrFetchResult.getErrors().add(
                            new IPOGrDTO.Error(
                                    ipoGr.getSlug(),
                                    ipoGr.getName(),
                                    "Invalid GMP Price: " + ipoGrGmp.getPrice()));
                    continue;
                }

                LocalDateTime lastUpdated = ipoGrGmp.getUpdatedAt()
                        .atZone(zoneId)
                        .toLocalDateTime();

                LocalDate gmpDate = lastUpdated.toLocalDate();

                LatestGMPProjection latestGmp = latestGmpMap.get(ipo.getId());

                if (latestGmp == null) {
                    gmpInserts.add(new GMPInsert(
                            ipo.getId(),
                            gmpPrice,
                            gmpDate,
                            lastUpdated));

                    ipoGrFetchResult.getSaved().add(
                            new IPOGrDTO.Saved(
                                    ipo.getId(),
                                    ipoGr.getName(),
                                    "Null Date GMP"));

                    continue;
                }

                LocalDate latestGmpDate = latestGmp.getGmpDate();

                if (latestGmpDate == null) {
                    gmpInserts.add(new GMPInsert(
                            ipo.getId(),
                            gmpPrice,
                            gmpDate,
                            lastUpdated));

                    continue;
                }

                if (gmpDate.isAfter(latestGmpDate)) {
                    gmpInserts.add(new GMPInsert(
                            ipo.getId(),
                            gmpPrice,
                            gmpDate,
                            lastUpdated));

                    ipoGrFetchResult.getSaved().add(
                            new IPOGrDTO.Saved(
                                    ipo.getId(),
                                    ipoGr.getName(),
                                    "New Date GMP"));

                    continue;
                }

                if (gmpDate.isEqual(latestGmpDate)) {

                    boolean priceChanged = Double.compare(latestGmp.getGmp(), gmpPrice) != 0;

                    boolean updatedAtChanged = latestGmp.getLastUpdated() == null
                            || lastUpdated.isAfter(latestGmp.getLastUpdated());

                    if (priceChanged || updatedAtChanged) {
                        gmpUpdates.add(new GMPUpdate(
                                ipo.getId(),
                                gmpDate,
                                gmpPrice,
                                lastUpdated));

                        ipoGrFetchResult.getExists().add(
                                new IPOGrDTO.Exists(
                                        ipo.getId().toString(),
                                        ipo.getName() + " (GMP updated)"));
                    }
                }
            } catch (Exception e) {
                ipoGrFetchResult.getErrors().add(
                        new IPOGrDTO.Error(
                                ipoGr.getSlug(),
                                ipoGr.getName(),
                                "GMP Exception: " + e.getMessage()));
            }
        }

        if (!gmpInserts.isEmpty()) {
            gmpBatchRepository.insertGmpBatch(gmpInserts);
        }

        if (!gmpUpdates.isEmpty()) {
            gmpBatchRepository.updateGmpBatch(gmpUpdates);
        }
    }

    private void updateListing(
            List<IPOGr> ipoGrData,
            Map<String, IPO> slugIpoMap,
            IPOGrFetchResult ipoGrFetchResult) {

        if (slugIpoMap.isEmpty()) {
            return;
        }

        List<IPO> updateIpos = new ArrayList<>();

        for (IPOGr ipoGr : ipoGrData) {
            try {
                IPO ipo = slugIpoMap.get(ipoGr.getSlug());

                if (ipo == null) {
                    continue;
                }

                if (ipoGr.getIsListed() == null
                        || !ipoGr.getIsListed()) {
                    continue;
                }

                String listingPrice = ipoGr.getListingPrice();

                if (listingPrice == null || listingPrice.isBlank()) {
                    continue;
                }

                double price;

                try {
                    price = Double.parseDouble(listingPrice);
                } catch (NumberFormatException e) {
                    ipoGrFetchResult.getErrors().add(
                            new IPOGrDTO.Error(
                                    ipoGr.getSlug(),
                                    ipoGr.getName(),
                                    "Invalid Listing Price: " + listingPrice));
                    continue;
                }

                if (ipo.getListedPrice() != null
                        && Double.compare(ipo.getListedPrice(), price) == 0) {
                    continue;
                }

                ipo.setListedPrice(price);

                updateIpos.add(ipo);

                ipoGrFetchResult.getExists().add(
                        new IPOGrDTO.Exists(
                                ipo.getId().toString(),
                                ipo.getName() + " (Listing Price updated)"));

            } catch (Exception e) {
                ipoGrFetchResult.getErrors().add(
                        new IPOGrDTO.Error(
                                ipoGr.getSlug(),
                                ipoGr.getName(),
                                "Listing Exception: " + e.getMessage()));
            }
        }

        if (!updateIpos.isEmpty()) {
            ipoRepository.saveAll(updateIpos);
        }
    }

    public void summaryMail(IPOGrFetchResult ipoGrFetchResult, String userEmail) {
        List<IPOGrDTO.Saved> saved = ipoGrFetchResult.getSaved();
        List<IPOGrDTO.Skipped> skipped = ipoGrFetchResult.getSkipped();
        List<IPOGrDTO.Exists> exists = ipoGrFetchResult.getExists();
        List<IPOGrDTO.Error> errors = ipoGrFetchResult.getErrors();

        Map<String, Object> summary = new LinkedHashMap<>();

        int total = saved.size() + skipped.size() + exists.size() + errors.size();

        summary.put("Total", total);
        summary.put("Total Saved", saved.size());
        summary.put("Total Exists", exists.size());
        summary.put("Total Skipped", skipped.size());
        summary.put("Total Errors", errors.size());
        summary.put("Saved Ipos", saved);
        summary.put("Exists Ipos", exists);
        summary.put("Skipped Ipos", skipped);
        summary.put("Errors Ipos", errors);

        mailService.sendIpoFetchSummaryEmail(userEmail, summary);
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledIPOFetch() {
        fetchIPOInBackground("open", null, 1, adminMail);
    }

    @Scheduled(cron = "0 0 8,11,16,18,23 * * *", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 30 9,12,14,17 * * *", zone = "Asia/Kolkata")
    public void scheduledIPOUpdate() {
        fetchAndUpdateIPOInBackground(null, null, 1, null, adminMail);
    }

}
