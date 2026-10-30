package org.shopwave.smartclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the single administrative account for the clinic.
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>Only one Admin row is expected to ever exist; it is seeded at setup, not self-registered.</li>
 *   <li>Passwords are never stored in plain text — the field holds a BCrypt hash.</li>
 *   <li>{@code email} is required by the schema even though your request only listed {@code username} and {@code password}.</li>
 * </ul>
 *
 * @JsonProperty on {@code passwordHash} ensures the hash is write-only (never returned to the client).
 */
@Entity
@Table(name = "admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotNull
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
