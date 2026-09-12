package com.virtu_stock.Controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.DTO.Response.IPOSearchResponse;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.Models.IPO;
import com.virtu_stock.Service.IPOService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/ipo")
@RequiredArgsConstructor
public class IPOController {

    private final IPOService ipoService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<PageResponseDTO<IPOResponseDTO>> findAll(
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE) int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        // Integer.parseInt(AppConstants.PAGE_SIZE)

        return ResponseEntity.ok(ipoService.findAll(page, size, sortBy, sortDir));
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<IPOResponseDTO>> findByStatus(@RequestParam String status) {
        List<IPO> ipos = ipoService.findByStatus(status);
        List<IPOResponseDTO> iposDTO = ipos.stream().map(ipo -> modelMapper.map(ipo, IPOResponseDTO.class)).toList();
        iposDTO.forEach(IPOResponseDTO::normalizeSubscriptionsOrder);
        return ResponseEntity.ok(iposDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IPOResponseDTO> findById(@PathVariable UUID id) {
        IPO ipo = ipoService.findById(id);
        IPOResponseDTO ipoRes = modelMapper.map(ipo, IPOResponseDTO.class);
        ipoRes.normalizeSubscriptionsOrder();
        return ResponseEntity.ok(ipoRes);
    }

    @GetMapping("/search")
    public ResponseEntity<List<IPOSearchResponse>> searchQuery(@RequestParam String query) {
        List<IPO> search = ipoService.search(query);
        List<IPOSearchResponse> searchRes = search.stream().map(ipo -> modelMapper.map(ipo, IPOSearchResponse.class))
                .toList();
        return ResponseEntity.ok(searchRes);
    }

}
