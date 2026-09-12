package com.virtu_stock.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.Controllers.AppliedIpo;
import com.virtu_stock.DTO.Response.AllotedIPOResponseDTO;
import com.virtu_stock.DTO.Response.AppliedIpoResponseDTO;
import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidPaginationParameterException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidSortFieldException;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.User;
import com.virtu_stock.Repository.AppliedIpoRepository;
import com.virtu_stock.Exceptions.CustomExceptions.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppliedIpoService {
    private final AppliedIpoRepository appliedIpoRepository;
    private final ModelMapper modelMapper;

    public AppliedIpo save(AppliedIpo appliedIpo) {
        return appliedIpoRepository.save(appliedIpo);
    }

    public List<AppliedIpo> findAll() {
        return appliedIpoRepository.findAll();
    }

    public PageResponseDTO<AppliedIpoResponseDTO> findByUser(User user, int pageNumber, int pageSize, String sortBy,
            String sortDir) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new InvalidPaginationParameterException(
                    "Page number and size must be positive");
        }

        if (pageSize > AppConstants.PAGE_SIZE_MAX_LIMIT) {
            throw new InvalidPaginationParameterException(
                    "Page size cannot exceed " + AppConstants.PAGE_SIZE_MAX_LIMIT);
        }

        List<String> allowedSortFields = List.of(
                "ipo.startDate",
                "ipo.name",
                "appliedDate");

        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidSortFieldException(
                    "Invalid sort field. Allowed values: " + allowedSortFields);
        }
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<AppliedIpo> pageDetails = appliedIpoRepository.findByUser(user, pageable);
        List<AppliedIpo> appliedIpos = pageDetails.getContent();

        List<AppliedIpoResponseDTO> appliedIpoResponseDTOs = appliedIpos.stream().map(a -> {
            AppliedIpoResponseDTO appliedIpoResponseDTO = new AppliedIpoResponseDTO();
            appliedIpoResponseDTO.setId(a.getId());
            appliedIpoResponseDTO.setIpo(modelMapper.map(a.getIpo(), IPOResponseDTO.class));
            appliedIpoResponseDTO.setAppliedLot(a.getAppliedLot());
            appliedIpoResponseDTO.setAllotment(a.getAllotment());
            appliedIpoResponseDTO.setAppliedDate(a.getAppliedDate());
            if (a.getAllotedIpo() != null) {
                appliedIpoResponseDTO.setAllotedIpo(modelMapper.map(a.getAllotedIpo(), AllotedIPOResponseDTO.class));
            }
            return appliedIpoResponseDTO;
        }).toList();

        PageResponseDTO<AppliedIpoResponseDTO> appliedIpoPageResponseDTO = new PageResponseDTO<>();
        appliedIpoPageResponseDTO.setContent(appliedIpoResponseDTOs);
        appliedIpoPageResponseDTO.setPageNumber(pageDetails.getNumber());
        appliedIpoPageResponseDTO.setPageSize(pageDetails.getSize());
        appliedIpoPageResponseDTO.setTotalPageElements(pageDetails.getNumberOfElements());
        appliedIpoPageResponseDTO.setTotalPages(pageDetails.getTotalPages());
        appliedIpoPageResponseDTO.setTotalElements(pageDetails.getTotalElements());
        appliedIpoPageResponseDTO.setLastPage(pageDetails.isLast());
        return appliedIpoPageResponseDTO;
    }

    public boolean existsByUserAndIpo(User user, IPO ipo) {
        return appliedIpoRepository.existsByUserAndIpo(user, ipo);
    }

    public Optional<AppliedIpo> findByUserAndIpo(User user, IPO ipo) {
        return appliedIpoRepository.findByUserAndIpo(user, ipo);

    }

    public AppliedIpo findById(UUID id) {
        return appliedIpoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applied Ipo", "Id", id));
    }

    @Transactional
    public void deleteByUserAndIpo(User user, IPO ipo) {
        appliedIpoRepository.deleteByUserAndIpo(user, ipo);
    }

    public List<Object[]> countAppliedByUserAndMonthAndYear(UUID id, int year) {
        return appliedIpoRepository.countAppliedByUserMonthYear(id, year);
    }

    public List<Object[]> countAllotedByUserMonthAndYear(UUID id, int year) {
        return appliedIpoRepository.countAllotedByUserMonthYear(id, year);
    }
}
