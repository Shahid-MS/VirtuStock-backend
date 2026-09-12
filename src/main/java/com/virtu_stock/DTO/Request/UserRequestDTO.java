package com.virtu_stock.DTO.Request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @Size(min = 3, max = 20, message = "First name must be 3 characters long")
    private String firstName;

    @Size(max = 20, message = "Last name cant be greater than 20 characters long")
    private String lastName;

    @Pattern(regexp = "^(https?://)?(www\\.)?linkedin\\.com/.*$", message = "Invalid LinkedIn profile URL")
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$", message = "Invalid Instagram profile URL")
    private String instagramUrl;
    
    private String profilePicUrl;

}
