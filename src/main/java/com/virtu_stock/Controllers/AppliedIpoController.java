package com.virtu_stock.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Request.AppliedIpoRequestDTO;
import com.virtu_stock.DTO.Response.AllotedIPOResponseDTO;
import com.virtu_stock.DTO.Response.AppliedIpoResponseDTO;
import com.virtu_stock.DTO.Response.CheckAppliedIpoResponseDTO;
import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.Enum.AllotmentStatus;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;
import com.virtu_stock.Models.AllotedIpo;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Models.User;
import com.virtu_stock.Service.AllotedIpoService;
import com.virtu_stock.Service.AppliedIpoService;
import com.virtu_stock.Service.IPOService;
import com.virtu_stock.Service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

import java.util.Map;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AppliedIpoController {
    private final UserService userService;
    private final IPOService ipoService;
    private final AppliedIpoService appliedIpoService;
    private final ModelMapper modelMapper;
    private final AllotedIpoService allotedIpoService;

    @PostMapping("/apply")
    public ResponseEntity<?> markAsApplied(@Valid @RequestBody AppliedIpoRequestDTO request, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        IPO ipo = ipoService.findById(request.getIpoId());

        boolean alreadyApplied = appliedIpoService.existsByUserAndIpo(user, ipo);
        if (alreadyApplied) {
            throw new BadRequestException("You have already applied for this IPO");
        }

        AppliedIpo appliedIpo = new AppliedIpo();
        appliedIpo.setUser(user);
        appliedIpo.setIpo(ipo);
        appliedIpo.setAppliedLot(request.getAppliedLot());
        appliedIpo.setAllotment(AllotmentStatus.NOT_ALLOTED);
        appliedIpoService.save(appliedIpo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Successfully marked as apply", "appliedIpoId", appliedIpo.getId()));
    }

    @GetMapping("/check-applied-ipo")
    public ResponseEntity<?> checkApplied(
            Principal principal,
            @RequestParam UUID ipoId) {

        String email = principal.getName();
        User user = userService.findByEmail(email);
        IPO ipo = ipoService.findById(ipoId);

        return appliedIpoService.findByUserAndIpo(user, ipo)
                .map(a -> ResponseEntity.ok(new CheckAppliedIpoResponseDTO(true, a.getId())))
                .orElse(ResponseEntity.ok(new CheckAppliedIpoResponseDTO(false, null)));

    }

    @DeleteMapping("/unmark-as-applied")
    public ResponseEntity<?> unmarkApplied(
            Principal principal,
            @RequestParam UUID ipoId) {

        String email = principal.getName();
        User user = userService.findByEmail(email);
        IPO ipo = ipoService.findById(ipoId);

        boolean applied = appliedIpoService.existsByUserAndIpo(user, ipo);
        if (!applied) {
            throw new BadRequestException("You have not applied to this IPO");
        }
        appliedIpoService.deleteByUserAndIpo(user, ipo);
        return ResponseEntity.ok(Map.of(
                "message", "Unmarked as applied successfully"));
    }

    @GetMapping("/applied-ipo")
    public ResponseEntity<?> findByUser(Principal principal,
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE) int size,
            @RequestParam(defaultValue = "ipo.startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        PageResponseDTO<AppliedIpoResponseDTO> pageResponseDTO = appliedIpoService.findByUser(user, page, size, sortBy,
                sortDir);

        return ResponseEntity.ok(pageResponseDTO);
    }

    @GetMapping("/applied-ipo/{id}")
    public ResponseEntity<?> findById(@PathVariable UUID id, Principal principal) {

        String email = principal.getName();
        User user = userService.findByEmail(email);
        AppliedIpo appliedIpo = appliedIpoService.findById(id);
        if (!appliedIpo.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to access this Applied IPO: " + id);
        }
        AppliedIpoResponseDTO res = modelMapper.map(appliedIpo, AppliedIpoResponseDTO.class);
        if (appliedIpo.getIpo() != null) {
            res.setIpo(modelMapper.map(appliedIpo.getIpo(), IPOResponseDTO.class));
        }
        if (appliedIpo.getAllotedIpo() != null) {
            res.setAllotedIpo(modelMapper.map(appliedIpo.getAllotedIpo(), AllotedIPOResponseDTO.class));
        }
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/applied-ipo/{id}")
    public ResponseEntity<?> updateById(@PathVariable UUID id, Principal principal,
            @RequestBody Map<String, Object> req) {

        String email = principal.getName();
        User user = userService.findByEmail(email);
        AppliedIpo appliedIpo = appliedIpoService.findById(id);
        if (!appliedIpo.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to access this Applied IPO: " + id);
        }

        for (String key : req.keySet()) {
            switch (key) {
                case "lot" -> {
                    Integer lot = (Integer) req.get("lot");

                    if (lot == null || lot < 1) {
                        throw new BadRequestException("Invalid lot value: " + lot);
                    }

                    if (!lot.equals(appliedIpo.getAppliedLot())) {
                        appliedIpo.setAppliedLot(lot);
                    }
                }
                case "allotment" -> {
                    String allotmentStr = req.get("allotment").toString();
                    AllotmentStatus status = AllotmentStatus.valueOf(allotmentStr);

                    if (appliedIpo.getAllotment() != status) {
                        if (status == AllotmentStatus.ALLOTED) {
                            AllotedIpo allotedIpo = allotedIpoService.createAllotment(appliedIpo.getId());
                            appliedIpo.setAllotedIpo(allotedIpo);

                        } else if (status == AllotmentStatus.NOT_ALLOTED) {
                            allotedIpoService.deleteAllotment(appliedIpo.getId());

                        }
                        appliedIpo.setAllotment(status);
                    }
                }
                default -> {
                    throw new BadRequestException("Invalid field: " + key);
                }
            }
        }
        appliedIpoService.save(appliedIpo);
        AppliedIpoResponseDTO appliedIpoResponseDTO = modelMapper.map(appliedIpo, AppliedIpoResponseDTO.class);
        return ResponseEntity.ok(Map.of("message", "Updated Successfully", "appliedIpo", appliedIpoResponseDTO));
    }

}
