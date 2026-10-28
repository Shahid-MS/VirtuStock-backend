package com.virtu_stock.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.virtu_stock.Enum.IPOStatus;
import com.virtu_stock.Enum.Verdict;
import com.virtu_stock.Models.IPO;

import jakarta.persistence.criteria.Predicate;

public class IPOSpecification {

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

        public static Specification<IPO> hasStatuses(Set<IPOStatus> statuses) {

                return (root, query, cb) -> {

                        if (statuses == null || statuses.isEmpty()) {
                                return cb.conjunction();
                        }

                        LocalDate today = LocalDate.now();

                        List<Predicate> statusPredicates = new ArrayList<>();

                        for (IPOStatus status : statuses) {

                                switch (status) {

                                        case UPCOMING:
                                                statusPredicates.add(
                                                                cb.greaterThan(
                                                                                root.get("startDate"), today));
                                                break;

                                        case OPEN:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("startDate"),
                                                                                                today),
                                                                                cb.greaterThan(
                                                                                                root.get("endDate"),
                                                                                                today)));
                                                break;

                                        case ALLOTMENT_PENDING:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("endDate"),
                                                                                                today),
                                                                                cb.greaterThan(
                                                                                                root.get("allotmentDate"),
                                                                                                today)));
                                                break;

                                        case ALLOTMENT:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("endDate"),
                                                                                                today),
                                                                                cb.equal(
                                                                                                root.get("allotmentDate"),
                                                                                                today)));
                                                break;

                                        case LISTING_PENDING:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("endDate"),
                                                                                                today),
                                                                                cb.lessThan(
                                                                                                root.get("allotmentDate"),
                                                                                                today),
                                                                                cb.greaterThan(
                                                                                                root.get("listingDate"),
                                                                                                today)));
                                                break;

                                        case LISTED:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("endDate"),
                                                                                                today),
                                                                                cb.lessThan(
                                                                                                root.get("allotmentDate"),
                                                                                                today),
                                                                                cb.equal(
                                                                                                root.get("listingDate"),
                                                                                                today),
                                                                                cb.isNotNull(
                                                                                                root.get("listedPrice"))));
                                                break;

                                        case CLOSED:
                                                statusPredicates.add(
                                                                cb.and(
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("startDate"),
                                                                                                today),
                                                                                cb.lessThanOrEqualTo(
                                                                                                root.get("endDate"),
                                                                                                today),
                                                                                cb.lessThan(
                                                                                                root.get("allotmentDate"),
                                                                                                today),
                                                                                cb.or(
                                                                                                cb.lessThan(
                                                                                                                root.get("listingDate"),
                                                                                                                today),
                                                                                                cb.and(
                                                                                                                cb.equal(
                                                                                                                                root.get("listingDate"),
                                                                                                                                today),
                                                                                                                cb.isNull(
                                                                                                                                root.get("listedPrice"))))));
                                                break;
                                }
                        }

                        return cb.or(statusPredicates.toArray(new Predicate[0]));
                };
        }

}
