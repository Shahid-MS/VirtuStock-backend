package com.virtu_stock.Controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.virtu_stock.Configurations.AppConstants;
import com.virtu_stock.DTO.Request.IPOUpdateRequestDTO;
import com.virtu_stock.DTO.Response.IPOResponseDTO;
import com.virtu_stock.DTO.Response.PageResponseDTO;
import com.virtu_stock.DTO.Response.UserResponseDTO;
import com.virtu_stock.Enum.Role;
import com.virtu_stock.Models.User;
import com.virtu_stock.Service.AsyncService;
import com.virtu_stock.Service.IPOService;
import com.virtu_stock.Service.UserService;
import com.virtu_stock.Util.AuthUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IPOService ipoService;
    private final AsyncService asyncService;
    private final UserService userService;

    @PostMapping("/ipo/fetch")
    public ResponseEntity<Map<String, Object>> fetchIPO(
            @RequestParam(required = false, defaultValue = "open") String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int limit) {
        String email = AuthUtil.getCurrentUserEmail();

        asyncService.fetchIPOInBackground(status, type, limit, email);

        return ResponseEntity.ok(
                Map.of("message", "IPO fetch process started. You will receive an email once the task is completed"));
    }

    @PutMapping("/ipo/{id}")
    public ResponseEntity<?> updateIpo(@PathVariable UUID id, @RequestBody IPOUpdateRequestDTO ipoReq) {
        IPOResponseDTO ipo = ipoService.updateIpo(id, ipoReq);
        return ResponseEntity.ok(Map.of("message", "Updated Successfully", "IPO", ipo));
    }

    @GetMapping("/count")
    public ResponseEntity<?> countUsers() {
        long countUsers = userService.countUsers();
        double userPercentageGrowth = userService.userPercentageGrowth();
        double roundedValueUserPercentageGrowth = BigDecimal
                .valueOf(userPercentageGrowth)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        long countIpo = ipoService.countIpos();
        double ipoPercentageGrowth = ipoService.ipoPercentageGrowth();
        double roundedValueIpoPercentageGrowth = BigDecimal
                .valueOf(ipoPercentageGrowth)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return ResponseEntity
                .ok(Map.of("totalUsers", countUsers, "userPercentageGrowth", roundedValueUserPercentageGrowth,
                        "totalIpos", countIpo, "ipoPercentageGrowth", roundedValueIpoPercentageGrowth));
    }

    @GetMapping("/user")
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> findAllUsers(
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        // Integer.parseInt(AppConstants.PAGE_SIZE)

        return ResponseEntity.ok(userService.findAll(page, size, sortBy, sortDir));
    }

    @PostMapping("/user/role")
    public ResponseEntity<?> assignAdmin(
            @RequestParam UUID userId,
            @RequestParam boolean assignAdmin) {
        User user = userService.findById(userId);

        Set<Role> roles = user.getRoles();

        if (assignAdmin) {
            roles.add(Role.ROLE_ADMIN);
        } else {
            roles.remove(Role.ROLE_ADMIN);
        }

        user.setRoles(roles);
        userService.save(user);

        return ResponseEntity.ok(
                Map.of("message",
                        assignAdmin
                                ? user.getFirstName() + " promoted to Admin"
                                : "Admin role removed from " + user.getFirstName()));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        userService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @DeleteMapping("/ipo/{id}")
    public ResponseEntity<?> deleteIpo(@PathVariable UUID id) {
        ipoService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Ipo deleted successfully"));
    }
}
