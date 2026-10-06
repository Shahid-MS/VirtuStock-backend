package com.virtu_stock.Helper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.IssueSize;

@Component
public class IPOHelper {
    @SuppressWarnings("unchecked")
    public IPO mapToIPO(Object ipoObj) {
        if (!(ipoObj instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, Object> ipoMap = (Map<String, Object>) ipoObj;

        IPO ipo = new IPO();
        ipo.setIpoAlertId((String) ipoMap.get("id"));
        ipo.setName((String) ipoMap.get("name"));
        ipo.setSymbol((String) ipoMap.get("symbol"));
        ipo.setType((String) ipoMap.get("type"));

        ipo.setStartDate(LocalDate.parse((String) ipoMap.get("startDate")));
        ipo.setEndDate(LocalDate.parse((String) ipoMap.get("endDate")));
        ipo.setListingDate(LocalDate.parse((String) ipoMap.get("listingDate")));

        String[] priceRange = ((String) ipoMap.get("priceRange")).split("-");
        ipo.setMinPrice(Double.parseDouble(priceRange[0]));
        ipo.setMaxPrice(Double.parseDouble(priceRange[1]));

        ipo.setMinQty((Integer) ipoMap.get("minQty"));
        ipo.setLogo((String) ipoMap.get("logo"));
        ipo.setIssueSize(new IssueSize("NA", "NA", (String) ipoMap.get("issueSize")));

        ipo.setAbout((String) ipoMap.get("about"));

        Object strengthsObj = ipoMap.get("strengths");
        ipo.setStrengths(((List<?>) strengthsObj).stream()
                .map(String::valueOf)
                .toList());

        Object risksObj = ipoMap.get("risks");
        ipo.setRisks(((List<?>) risksObj).stream()
                .map(String::valueOf)
                .toList());

        List<Map<String, Object>> scheduleList = (List<Map<String, Object>>) ipoMap.get("schedule");
        if (scheduleList != null) {
            for (Map<String, Object> eventMap : scheduleList) {
                String event = (String) eventMap.get("event");
                if ("Allotment finalization".equalsIgnoreCase(event)) {
                    String dateStr = (String) eventMap.get("date");
                    if (dateStr != null && !dateStr.isBlank()) {
                        ipo.setAllotmentDate(LocalDate.parse(dateStr));
                    }
                    break;
                }
            }
        }

        return ipo;
    }

    public void addSubscription(
            Map<String, Double> subscriptions,
            String name,
            String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        subscriptions.put(name, Double.parseDouble(value));
    }

    public boolean isMatchingIPO(IPO ipo1, IPO ipo2) {

        if (!Objects.equals(ipo1.getType(), ipo2.getType())) {
            return false;
        }

        int score = 0;

        String name1 = ipo1.getName().trim().toLowerCase();
        String name2 = ipo2.getName().trim().toLowerCase();

        String firstWord1 = name1.split("\\s+")[0];
        String firstWord2 = name2.split("\\s+")[0];

        if (name1.startsWith(firstWord2) || name2.startsWith(firstWord1)) {
            score += 40;
        }

        if (Objects.equals(ipo1.getStartDate(), ipo2.getStartDate())) {
            score += 30;
        }

        if (Objects.equals(ipo1.getEndDate(), ipo2.getEndDate())) {
            score += 15;
        }

        if (Objects.equals(ipo1.getMinPrice(), ipo2.getMinPrice())) {
            score += 7;
        }

        if (Objects.equals(ipo1.getMaxPrice(), ipo2.getMaxPrice())) {
            score += 8;
        }

        return score >= 70;
    }

}
