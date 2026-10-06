package com.virtu_stock.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Request.IPOUpdateRequestDTO;
import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.Enum.IPOStatus;
import com.virtu_stock.Enum.Verdict;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidPaginationParameterException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidSortFieldException;
import com.virtu_stock.Exceptions.CustomExceptions.ResourceNotFoundException;
import com.virtu_stock.Models.GMP;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Projection.LatestGMPProjection;
import com.virtu_stock.Projection.SubscriptionProjection;
import com.virtu_stock.Repository.IPOProfileRepository;
import com.virtu_stock.Repository.IPORepository;
import com.virtu_stock.Specification.IPOSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IPOService {

    private final IPORepository ipoRepository;
    private final IPOProfileRepository ipoProfileRepository;

    @Qualifier("modelMapper")
    private final ModelMapper modelMapper;
    @Qualifier("ipoModelMapper")
    private final ModelMapper ipoModelMapper;

    public PageResponseDTO<IPOResponseDTO> findAll(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDir,
            String status,
            Verdict verdict,
            String type,
            String search) {

        if (pageNumber < 0 || pageSize <= 0) {
            throw new InvalidPaginationParameterException(
                    "Page number and size must be positive");
        }

        if (pageSize > AppConstants.PAGE_SIZE_MAX_LIMIT) {
            throw new InvalidPaginationParameterException(
                    "Page size cannot exceed " + AppConstants.PAGE_SIZE_MAX_LIMIT);
        }

        List<String> allowedSortFields = List.of("startDate", "name");

        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidSortFieldException(
                    "Invalid sort field. Allowed values: " + allowedSortFields);
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Specification<IPO> specification = null;

        if (status != null && !status.isBlank()) {
            specification = IPOSpecification.hasStatus(status);
        }

        if (verdict != null) {
            specification = specification == null
                    ? IPOSpecification.hasVerdict(verdict)
                    : specification.and(IPOSpecification.hasVerdict(verdict));
        }

        if (type != null && !type.isBlank()) {
            specification = specification == null
                    ? IPOSpecification.hasType(type)
                    : specification.and(IPOSpecification.hasType(type));
        }

        if (search != null && !search.isBlank()) {
            specification = specification == null
                    ? IPOSpecification.search(search)
                    : specification.and(IPOSpecification.search(search));
        }

        Page<IPO> pageDetails = specification != null
                ? ipoRepository.findAll(specification, pageable)
                : ipoRepository.findAll(pageable);

        List<IPOResponseDTO> iposDTO = enrichIPOResponses(pageDetails.getContent());

        PageResponseDTO<IPOResponseDTO> response = new PageResponseDTO<>();

        response.setContent(iposDTO);
        response.setPageNumber(pageDetails.getNumber());
        response.setPageSize(pageDetails.getSize());
        response.setTotalPageElements(pageDetails.getNumberOfElements());
        response.setTotalPages(pageDetails.getTotalPages());
        response.setTotalElements(pageDetails.getTotalElements());
        response.setLastPage(pageDetails.isLast());

        return response;
    }

    public List<IPOResponseDTO> findByStatus(String status) {

        IPOStatus ipoStatus;

        try {
            ipoStatus = IPOStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid IPO status: " + status);
        }

        Specification<IPO> specification = IPOSpecification.hasStatus(ipoStatus.name());

        List<IPO> ipos = ipoRepository.findAll(specification);

        return enrichIPOResponses(ipos);
    }

    private List<IPOResponseDTO> enrichIPOResponses(List<IPO> ipos) {

        if (ipos == null || ipos.isEmpty()) {
            return List.of();
        }

        List<UUID> ipoIds = ipos.stream()
                .map(IPO::getId)
                .toList();

        // Fetch latest GMP for all IPOs in one query
        List<LatestGMPProjection> latestGmps = ipoProfileRepository.findLatestGmp(ipoIds);

        Map<UUID, GMP> latestGmpMap = latestGmps.stream()
                .collect(Collectors.toMap(
                        gmp -> UUID.fromString(gmp.getIpoId()),
                        gmp -> GMP.builder()
                                .gmp(gmp.getGmp())
                                .gmpDate(gmp.getGmpDate())
                                .lastUpdated(gmp.getLastUpdated())
                                .build()));

        // Fetch subscriptions for all IPOs in one query
        List<SubscriptionProjection> subscriptions = ipoProfileRepository.findSubscription(ipoIds);

        Map<UUID, SubscriptionProjection> subscriptionMap = subscriptions.stream()
                .collect(Collectors.toMap(
                        sub -> UUID.fromString(sub.getIpoId()),
                        sub -> sub));

        // Map IPO + GMP + Subscription
        List<IPOResponseDTO> iposDTO = ipos.stream()
                .map(ipo -> {

                    IPOResponseDTO dto = ipoModelMapper.map(ipo, IPOResponseDTO.class);

                    // Latest GMP
                    GMP latestGmp = latestGmpMap.get(ipo.getId());

                    dto.setGmp(
                            latestGmp != null
                                    ? List.of(latestGmp)
                                    : List.of());

                    // Subscription
                    SubscriptionProjection subs = subscriptionMap.get(ipo.getId());

                    LinkedHashMap<String, Double> subscription = new LinkedHashMap<>();

                    subscription.put(
                            "QIB",
                            subs != null && subs.getQib() != null
                                    ? subs.getQib()
                                    : 0.0);

                    subscription.put(
                            "Non-Institutional",
                            subs != null && subs.getNonInstitutional() != null
                                    ? subs.getNonInstitutional()
                                    : 0.0);

                    subscription.put(
                            "Retailer",
                            subs != null && subs.getRetailer() != null
                                    ? subs.getRetailer()
                                    : 0.0);

                    subscription.put(
                            "Total",
                            subs != null && subs.getTotal() != null
                                    ? subs.getTotal()
                                    : 0.0);

                    dto.setSubscriptions(subscription);

                    return dto;
                })
                .toList();

        iposDTO.forEach(IPOResponseDTO::normalizeSubscriptionsOrder);

        return iposDTO;
    }

    public IPO findById(UUID id) {
        return ipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IPO", "id", id));
    }

    public IPO save(IPO ipo) {
        return ipoRepository.save(ipo);
    }

    public IPOResponseDTO updateIpo(UUID id, IPOUpdateRequestDTO ipoReq) {
        IPO ipo = findById(id);
        if (ipoReq.getSubscriptions() != null) {
            Set<String> PROTECTED_KEYS = Set.of(
                    "QIB", "Non-Institutional", "Retailer", "Total");
            for (String protectedKey : PROTECTED_KEYS) {
                if (!ipoReq.getSubscriptions().containsKey(protectedKey)) {
                    throw new BadRequestException(
                            "Subscription '" + protectedKey + "' cannot be deleted");
                }
            }

            ipo.getSubscriptions().keySet().removeIf(key -> !PROTECTED_KEYS.contains(key) &&
                    !ipoReq.getSubscriptions().containsKey(key));
            ipo.setSubscriptions(ipoReq.getSubscriptions());
            ipo.setSubscriptionLastUpdated(LocalDateTime.now());
        }

        if (ipoReq.getGmp() != null) {
            List<GMP> existingGmp = ipo.getGmp();
            for (GMP g : ipoReq.getGmp()) {
                if (g.getGmpDate().isBefore(ipo.getStartDate()) || g.getGmpDate().isAfter(ipo.getListingDate())) {
                    throw new BadRequestException("GMP date " + g.getGmpDate() +
                            " must be between IPO open date (" + ipo.getStartDate() +
                            ") and listing date (" + ipo.getListingDate() + ")");
                }

                Optional<GMP> foundGMP = existingGmp.stream().filter(s -> s.getGmpDate().equals(g.getGmpDate()))
                        .findFirst();
                if (foundGMP.isPresent()) {
                    if (foundGMP.get().getGmp() != g.getGmp()) {
                        foundGMP.get().setGmp(g.getGmp());
                        foundGMP.get().setLastUpdated(LocalDateTime.now());
                    }
                } else {
                    existingGmp.add(GMP.builder().gmp(g.getGmp()).gmpDate(g.getGmpDate())
                            .lastUpdated(LocalDateTime.now()).build());
                }
                ipo.setGmp(existingGmp);
            }
        }

        modelMapper.typeMap(IPOUpdateRequestDTO.class, IPO.class)
                .addMappings(mapper -> mapper.skip(IPO::setSubscriptions))
                .addMappings(mapper -> mapper.skip(IPO::setGmp));

        modelMapper.getConfiguration()
                .setSkipNullEnabled(true);

        modelMapper.map(ipoReq, ipo);

        IPO savedIpo = save(ipo);
        return modelMapper.map(savedIpo, IPOResponseDTO.class);
    }

    public void deleteById(UUID id) {
        if (!ipoRepository.existsById(id)) {
            throw new ResourceNotFoundException("IPO", "id", id);
        }
        ipoRepository.deleteById(id);
    }

    public List<IPO> fetchIPOByListingPending() {
        List<IPO> ipos = ipoRepository.findByListingDateLessThanEqual(LocalDate.now());
        return ipos;
    }

    public List<Object[]> getIpoCountByMonthAndYear(Integer year) {
        return ipoRepository.countIpoByMonthAndYear(year);
    }

    public List<IPO> search(String query) {
        query = query.trim();
        if (query.length() <= 2) {
            return List.of();
        }
        List<IPO> search = ipoRepository.findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCaseOrderByName(query,
                query);
        return search.stream().limit(7).toList();
    }

    public long countIpos() {
        return ipoRepository.count();
    }

    public double ipoPercentageGrowth() {
        LocalDate today = LocalDate.now();
        LocalDate startOfThisMonth = today.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfThisMonth.plusMonths(1);
        LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);

        long thisMonthIpos = ipoRepository.countByStartDateBetween(
                startOfThisMonth, startOfNextMonth.minusDays(1));

        long lastMonthIpos = ipoRepository.countByStartDateBetween(
                startOfLastMonth, startOfThisMonth.minusDays(1));

        if (lastMonthIpos == 0) {
            return thisMonthIpos > 0 ? 100.0 : 0.0;
        }

        return ((double) (thisMonthIpos - lastMonthIpos) / lastMonthIpos) * 100;
    }

}
