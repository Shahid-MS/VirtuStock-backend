package com.virtu_stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.virtu_stock.DTO.Response.IPOGrFinancialRow;
import com.virtu_stock.DTO.Response.IPOGrFinancials;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.IPOFinancial;
import com.virtu_stock.Repository.IPOFinancialRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IPOFinancialService {
    private final IPOFinancialRepository ipoFinancialRepository;

    private Map<LocalDate, Map<String, BigDecimal>> mapFinancials(
            IPOGrFinancials source) {

        Map<LocalDate, Map<String, BigDecimal>> result = new TreeMap<>(Comparator.reverseOrder());

        if (source == null || source.getHeaders() == null ||
                source.getRows() == null) {
            return result;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

        List<LocalDate> dates = new ArrayList<>();

        for (String header : source.getHeaders()) {
            if (header == null || header.isBlank()) {
                dates.add(null);
                continue;
            }

            try {
                dates.add(LocalDate.parse(header.trim(), formatter));
            } catch (DateTimeParseException e) {
                dates.add(null);
            }
        }

        for (IPOGrFinancialRow row : source.getRows()) {

            if (row == null || row.getMetric() == null ||
                    row.getMetric().isBlank() || row.getData() == null) {
                continue;
            }

            for (int i = 0; i < Math.min(dates.size(), row.getData().size()); i++) {

                LocalDate date = dates.get(i);
                String value = row.getData().get(i);

                if (date == null || value == null || value.isBlank()) {
                    continue;
                }

                try {
                    BigDecimal amount = new BigDecimal(
                            value.replace(",", "").trim());

                    result.computeIfAbsent(date, k -> new LinkedHashMap<>())
                            .put(row.getMetric().trim(), amount);

                } catch (NumberFormatException e) {
                    // Skip invalid financial values
                }
            }
        }

        return result;
    }

    @Transactional
    public boolean saveOrUpdateFinancials(IPO ipo, IPOGrFinancials source) {

        if (ipo == null || source == null) {
            return false;
        }

        Map<LocalDate, Map<String, BigDecimal>> newFinancials = mapFinancials(source);

        String newUnit = source.getUnit();

        if (newFinancials.isEmpty()) {
            return false;
        }

        Optional<IPOFinancial> existingOptional = ipoFinancialRepository.findByIpoId(ipo.getId());

        if (existingOptional.isEmpty()) {

            IPOFinancial financial = new IPOFinancial();
            financial.setIpo(ipo);
            financial.setFinancials(newFinancials);
            financial.setUnit(newUnit);

            ipoFinancialRepository.save(financial);

            System.out.println("Financials created for IPO: " + ipo.getId());
            return true;
        }

        IPOFinancial existing = existingOptional.get();

        boolean financialsChanged = !Objects.equals(existing.getFinancials(), newFinancials);

        boolean unitChanged = !Objects.equals(existing.getUnit(), newUnit);

        if (financialsChanged || unitChanged) {

            existing.setFinancials(newFinancials);
            existing.setUnit(newUnit);

            ipoFinancialRepository.save(existing);

            System.out.println("Financials updated for IPO: " + ipo.getId());
            return true;
        }

        return false;
    }
}
