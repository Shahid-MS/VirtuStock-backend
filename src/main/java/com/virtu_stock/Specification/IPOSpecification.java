package com.virtu_stock.Specification;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.virtu_stock.Enum.Verdict;
import com.virtu_stock.Models.IPO;

public class IPOSpecification {
    public static Specification<IPO> hasStatus(String status) {

        return (root, query, cb) -> {

            LocalDate today = LocalDate.now();

            return switch (status.toUpperCase()) {

                case "UPCOMING" ->
                    cb.greaterThan(
                            root.get("startDate"),
                            today);

                case "OPEN" ->
                    cb.and(
                            cb.lessThanOrEqualTo(
                                    root.get("startDate"),
                                    today),
                            cb.greaterThanOrEqualTo(
                                    root.get("endDate"),
                                    today));

                case "ALLOTMENT_PENDING" ->
                    cb.and(
                            cb.lessThan(
                                    root.get("endDate"),
                                    today),
                            cb.or(
                                    cb.isNull(
                                            root.get("allotmentDate")),
                                    cb.greaterThan(
                                            root.get("allotmentDate"),
                                            today)));

                case "ALLOTMENT" ->
                    cb.equal(
                            root.get("allotmentDate"),
                            today);

                case "LISTING_PENDING" ->
                    cb.and(
                            cb.isNotNull(
                                    root.get("listingDate")),
                            cb.lessThanOrEqualTo(
                                    root.get("listingDate"),
                                    today),
                            cb.isNull(
                                    root.get("listedPrice")));

                case "LISTED" ->
                    cb.isNotNull(
                            root.get("listedPrice"));

                default ->
                    throw new IllegalArgumentException(
                            "Invalid IPO status: " + status);
            };
        };
    }

    public static Specification<IPO> hasVerdict(Verdict verdict) {

        return (root, query, cb) -> cb.equal(
                root.get("verdict"),
                verdict);
    }

    public static Specification<IPO> hasType(String type) {

        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("type")),
                type.toLowerCase());
    }

    public static Specification<IPO> search(String search) {

        return (root, query, cb) -> {

            String pattern = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(
                            cb.lower(root.get("name")),
                            pattern),
                    cb.like(
                            cb.lower(root.get("symbol")),
                            pattern));
        };
    }
}
