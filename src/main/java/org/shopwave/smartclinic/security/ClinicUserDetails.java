package org.shopwave.smartclinic.security;

import org.shopwave.smartclinic.model.Admin;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.model.Patient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security {@link UserDetails} implementation that wraps any of the three
 * clinic account types (Admin, Doctor, Patient).
 * <p>
 * A single authentication provider is used for all roles. After login, the
 * user's {@code role} determines which dashboard they reach and which endpoints
 * they can access.
 */
public class ClinicUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final UserRole role;
    private final boolean active;

    public ClinicUserDetails(Long id, String username, String password,
                             UserRole role, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public static ClinicUserDetails fromAdmin(Admin admin) {
        return new ClinicUserDetails(
                admin.getId(), admin.getUsername(), admin.getPasswordHash(),
                UserRole.ROLE_ADMIN, true);
    }

    /** Wraps a {@link Doctor} entity. */
    public static ClinicUserDetails fromDoctor(Doctor doctor) {
        return new ClinicUserDetails(
                doctor.getId(), doctor.getEmail(), doctor.getPasswordHash(),
                UserRole.ROLE_DOCTOR,
                doctor.getIsActive() != null && doctor.getIsActive());
    }

    /** Wraps a {@link Patient} entity. */
    public static ClinicUserDetails fromPatient(Patient patient) {
        return new ClinicUserDetails(
                patient.getId(), patient.getEmail(), patient.getPasswordHash(),
                UserRole.ROLE_PATIENT,
                patient.getIsActive() != null && patient.getIsActive());
    }
}
