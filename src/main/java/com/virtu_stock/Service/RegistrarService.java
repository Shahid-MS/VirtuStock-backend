package com.virtu_stock.Service;

import java.util.List;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.DTO.Response.RegistrarResponseDTO;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidPaginationParameterException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidSortFieldException;
import com.virtu_stock.Exceptions.CustomExceptions.ResourceNotFoundException;
import com.virtu_stock.Models.Registrar;
import com.virtu_stock.Repository.RegistrarRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarService {

    private final ModelMapper modelMapper;
    private final RegistrarRepository registrarRepository;

    public Registrar findById(Long id) {
        return registrarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registrar", "id", id));
    }

    public Registrar save(Registrar registrar) {
        return registrarRepository.save(registrar);
    }

    public Registrar findOrCreate(
            String name,
            String slug,
            String url) {

        if (slug == null || slug.isBlank()) {
            return null;
        }

        return registrarRepository.findBySlug(slug)
                .map(registrar -> {

                    boolean changed = false;

                    if (!Objects.equals(registrar.getName(), name)) {
                        registrar.setName(name);
                        changed = true;
                    }

                    if (!Objects.equals(registrar.getUrl(), url)) {
                        registrar.setUrl(url);
                        changed = true;
                    }

                    return changed
                            ? registrarRepository.save(registrar)
                            : registrar;
                })
                .orElseGet(() -> {
                    Registrar registrar = new Registrar();
                    registrar.setName(name);
                    registrar.setSlug(slug);
                    registrar.setUrl(url);

                    return registrarRepository.save(registrar);
                });
    }

    public PageResponseDTO<RegistrarResponseDTO> findAll(int pageNumber, int pageSize, String sortBy, String sortDir) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new InvalidPaginationParameterException(
                    "Page number and size must be positive");
        }

        if (pageSize > AppConstants.PAGE_SIZE_MAX_LIMIT) {
            throw new InvalidPaginationParameterException(
                    "Page size cannot exceed " + AppConstants.PAGE_SIZE_MAX_LIMIT);
        }

        List<String> allowedSortFields = List.of("createdAt", "name");

        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidSortFieldException(
                    "Invalid sort field. Allowed values: " + allowedSortFields);
        }
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Registrar> pageDetails = registrarRepository.findAll(pageable);
        List<Registrar> registrars = pageDetails.getContent();
        List<RegistrarResponseDTO> registrarDTO = registrars.stream()
                .map(registrar -> modelMapper.map(registrar, RegistrarResponseDTO.class))
                .toList();

        PageResponseDTO<RegistrarResponseDTO> registrarPageResponseDTO = new PageResponseDTO<RegistrarResponseDTO>();
        registrarPageResponseDTO.setContent(registrarDTO);
        registrarPageResponseDTO.setPageNumber(pageDetails.getNumber());
        registrarPageResponseDTO.setPageSize(pageDetails.getSize());
        registrarPageResponseDTO.setTotalPageElements(pageDetails.getNumberOfElements());
        registrarPageResponseDTO.setTotalPages(pageDetails.getTotalPages());
        registrarPageResponseDTO.setTotalElements(pageDetails.getTotalElements());
        registrarPageResponseDTO.setLastPage(pageDetails.isLast());
        return registrarPageResponseDTO;
    }

}
