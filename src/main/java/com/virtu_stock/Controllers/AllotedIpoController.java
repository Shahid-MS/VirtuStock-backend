package com.virtu_stock.Controllers;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.DTO.Response.AllotedIPOResponseDTO;
import com.virtu_stock.Exceptions.CustomExceptions.BadRequestException;
import com.virtu_stock.Exceptions.CustomExceptions.UnauthorizedException;
import com.virtu_stock.Models.AllotedIpo;
import com.virtu_stock.Models.User;
import com.virtu_stock.Service.AllotedIpoService;
import com.virtu_stock.Service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/user")
public class AllotedIpoController {
    private final AllotedIpoService allotedIpoService;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @PatchMapping("/alloted-ipo/{id}")
    public ResponseEntity<?> updateById(@PathVariable UUID id, Principal principal,
            @RequestBody Map<String, Object> req) {

        String email = principal.getName();
        User user = userService.findByEmail(email);
        AllotedIpo allotedIpo = allotedIpoService.findById(id);

        if (!allotedIpo.getAppliedIpo().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("User does not own this Alloted IPO: " + id);
        }

        for (String key : req.keySet()) {
            switch (key) {
                case "lot" -> {
                    Integer lot = (Integer) req.get("lot");

                    if (lot == null || lot < 1) {
                        throw new BadRequestException("Invalid lot value");
                    }

                    if (!lot.equals(allotedIpo.getAllotedLot())) {
                        allotedIpo.setAllotedLot(lot);
                    }
                }
                case "sellPrice" -> {
                    Object sellPriceObj = req.get("sellPrice");
                    Double sellPrice = sellPriceObj == null ? null : Double.parseDouble(sellPriceObj.toString());
                    if (allotedIpo.getSellPrice() != sellPrice) {
                        allotedIpo.setSellPrice(sellPrice);
                    }
                }
                default -> {
                    throw new BadRequestException("Invalid field: " + key);
                }
            }
        }
        allotedIpoService.save(allotedIpo);
        AllotedIPOResponseDTO res = modelMapper.map(allotedIpo, AllotedIPOResponseDTO.class);
        return ResponseEntity.ok(Map.of("message", "Updated Successfully", "alloted", res));
    }

}
