
package com.virtu_stock.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class QueryRequestDTO {

    @NotBlank
    private String message;

    @Email(message = "Invalid email")
    private String email;
}