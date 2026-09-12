package com.virtu_stock.Helper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        ipo.setListedPrice(ipo.getMaxPrice());

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

}
