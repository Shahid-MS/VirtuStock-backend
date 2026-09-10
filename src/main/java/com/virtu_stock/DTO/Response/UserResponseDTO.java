package com.virtu_stock.DTO.Response;

import java.util.Set;
import java.util.UUID;

import com.virtu_stock.Enum.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<Role> roles;
    private String phone;
    private String instagramUrl;
    private String linkedinUrl;
    private String profilePicUrl;
}
