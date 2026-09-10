package com.virtu_stock.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Request.UserRequestDTO;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.DTO.Response.UserResponseDTO;
import com.virtu_stock.Enum.Role;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;
import com.virtu_stock.Exceptions.CustomExceptions.DuplicateResourceException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidPaginationParameterException;
import com.virtu_stock.Exceptions.CustomExceptions.InvalidSortFieldException;
import com.virtu_stock.Exceptions.CustomExceptions.ResourceNotFoundException;
import com.virtu_stock.Models.User;
import com.virtu_stock.Repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final ModelMapper modelMapper;

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(Role.ROLE_USER));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);

    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        return optionalUser.orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findById(UUID id) {
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    public User setPassword(String email, String password) {
        User user = findByEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public UserResponseDTO updateUser(User user, Map<String, Object> updates) {
        UserRequestDTO dto = new ObjectMapper().convertValue(updates, UserRequestDTO.class);
        Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            List<Map<String, String>> errors = violations.stream()
                    .map(v -> Map.of(
                            "field", v.getPropertyPath().toString(),
                            "message", v.getMessage()))
                    .toList();
            String errorMessage;
            try {
                errorMessage = new ObjectMapper().writeValueAsString(errors);
            } catch (JsonProcessingException e) {
                errorMessage = "Validation failed";
            }
            throw new BadRequestException(errorMessage.toString());
        }

        ModelMapper localMapper = new ModelMapper();
        localMapper.getConfiguration().setSkipNullEnabled(true);
        localMapper.map(dto, user);
        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public double userPercentageGrowth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfNextMonth = startOfThisMonth.plusMonths(1);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long thisMonthUsers = userRepository.countByCreatedAtBetween(
                startOfThisMonth, startOfNextMonth);

        long lastMonthUsers = userRepository.countByCreatedAtBetween(
                startOfLastMonth, startOfThisMonth);

        if (lastMonthUsers == 0) {
            return thisMonthUsers > 0 ? 100.0 : 0.0;
        }
        return ((double) (thisMonthUsers - lastMonthUsers) / lastMonthUsers) * 100;

    }

    public PageResponseDTO<UserResponseDTO> findAll(int pageNumber, int pageSize, String sortBy, String sortDir) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new InvalidPaginationParameterException(
                    "Page number and size must be positive");
        }

        if (pageSize > AppConstants.PAGE_SIZE_MAX_LIMIT) {
            throw new InvalidPaginationParameterException(
                    "Page size cannot exceed " + AppConstants.PAGE_SIZE_MAX_LIMIT);
        }

        List<String> allowedSortFields = List.of("createdAt", "firstName");

        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidSortFieldException(
                    "Invalid sort field. Allowed values: " + allowedSortFields);
        }
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<User> pageDetails = userRepository.findAll(pageable);
        List<User> users = pageDetails.getContent();
        List<UserResponseDTO> usersDTO = users.stream().map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();

        PageResponseDTO<UserResponseDTO> userPageResponseDTO = new PageResponseDTO<UserResponseDTO>();
        userPageResponseDTO.setContent(usersDTO);
        userPageResponseDTO.setPageNumber(pageDetails.getNumber());
        userPageResponseDTO.setPageSize(pageDetails.getSize());
        userPageResponseDTO.setTotalPageElements(pageDetails.getNumberOfElements());
        userPageResponseDTO.setTotalPages(pageDetails.getTotalPages());
        userPageResponseDTO.setTotalElements(pageDetails.getTotalElements());
        userPageResponseDTO.setLastPage(pageDetails.isLast());
        return userPageResponseDTO;
    }

}
