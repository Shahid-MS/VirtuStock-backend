package com.virtu_stock.Controllers;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.Models.User;
import com.virtu_stock.Service.AllotedIpoService;
import com.virtu_stock.Service.AppliedIpoService;
import com.virtu_stock.Service.IPOService;
import com.virtu_stock.Service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MonthlySummaryIPOController {
    private final UserService userService;
    private final IPOService ipoService;
    private final AppliedIpoService appliedIpoService;
    private final AllotedIpoService allotedIpoService;

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> monthlySummary(Principal principal, @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<Object[]> countIpoByMonthYear = ipoService.getIpoCountByMonthAndYear(year);
        List<Object[]> countAppliedIpoByUserMonthAndYear = appliedIpoService
                .countAppliedByUserAndMonthAndYear(user.getId(), year);
        List<Object[]> countAllotedIpoByUserMonthAndYear = appliedIpoService
                .countAllotedByUserMonthAndYear(user.getId(), year);

        Map<Integer, Integer> countTotalIpo = countIpoByMonthYear.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).intValue(),
                        r -> ((Number) r[1]).intValue()));

        Map<Integer, Integer> countAppliedIpo = countAppliedIpoByUserMonthAndYear.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).intValue(),
                        r -> ((Number) r[1]).intValue()));

        Map<Integer, Integer> countAllotedIpo = countAllotedIpoByUserMonthAndYear.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).intValue(),
                        r -> ((Number) r[1]).intValue()));

        List<String> months = new ArrayList<>();
        List<Integer> totalIpoList = new ArrayList<>();
        List<Integer> appliedList = new ArrayList<>();
        List<Integer> allotedList = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String monthName = Month.of(month)
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            months.add(monthName);
            totalIpoList.add(countTotalIpo.getOrDefault(month, 0));
            appliedList.add(countAppliedIpo.getOrDefault(month, 0));
            allotedList.add(countAllotedIpo.getOrDefault(month, 0));
        }

        Map<String, Object> yearData = new LinkedHashMap<>();
        yearData.put("month", months);
        yearData.put("totalIpo", totalIpoList);
        yearData.put("applied", appliedList);
        yearData.put("alloted", allotedList);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("Year-" + year, yearData);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly-summary-profit")
    public ResponseEntity<?> monthlySummaryProfit(Principal principal, @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<Object[]> monthlyProfitList = allotedIpoService.sumMonthlyProfit(user.getId(), year);

        Map<Integer, Integer> monthlyProfitMap = monthlyProfitList.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).intValue(),
                        r -> ((Number) r[1]).intValue()));
        List<String> months = new ArrayList<>();
        List<Integer> monthProfit = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            String monthName = Month.of(month)
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            months.add(monthName);

            monthProfit.add(monthlyProfitMap.getOrDefault(month, 0));
        }

        Map<String, Object> yearData = new LinkedHashMap<>();
        yearData.put("month", months);
        yearData.put("profit", monthProfit);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("Year-" + year, yearData);
        return ResponseEntity.ok(res);
    }

}