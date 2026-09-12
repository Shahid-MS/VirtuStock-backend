package com.virtu_stock.Models;

import java.time.LocalDateTime;
import java.util.HashSet;

import java.util.Set;
import java.util.UUID;

import com.virtu_stock.Enum.Role;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data

public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 - 20 characters long")
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = true, length = 15, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @Transient
    @NotBlank(message = "First name is required")
    @Size(min = 3, max = 20, message = "First name must be 3 characters long")
    private String firstName;

    @Transient
    @Size(max = 20, message = "Last name cant be greater than 20 characters long")
    private String lastName;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private boolean enabled;

    @Column(name = "created_At", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Pattern(regexp = "^(https?://)?(www\\.)?linkedin\\.com/.*$", message = "Invalid LinkedIn profile URL")
    @Column(nullable = true, length = 255)
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://)?(www\\.)?instagram\\.com/.*$", message = "Invalid Instagram profile URL")
    @Column(nullable = true, length = 255)
    private String instagramUrl;

    @Column
    private String profilePicUrl;

    public String getFullName() {
        return (fullName != null) ? fullName.replace("|", " ") : null;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    @PrePersist
    private void beforeSave() {
        updateFullName();
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    private void updateFullName() {
        if (firstName != null && lastName != null)
            this.fullName = firstName + "|" + lastName;
        else if (firstName != null)
            this.fullName = firstName;
        else
            this.fullName = null;
    }

    @PostLoad
    private void splitFullName() {
        if (fullName != null) {
            String[] parts = fullName.split("\\|", 2);
            this.firstName = parts[0];
            this.lastName = (parts.length > 1) ? parts[1] : "";
        }
    }

}
