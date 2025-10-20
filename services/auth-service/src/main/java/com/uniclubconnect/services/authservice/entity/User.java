package com.uniclubconnect.services.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails { // Spring Security'nin tanıması için

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID kullanmak ID'lerin tahmin edilmesini zorlaştırır
    private String id;

    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER) // Kullanıcı çekildiğinde rolleri de gelsin
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // --- UserDetails METOTLARI ---
    // Spring Security bu metotları kullanarak yetkilendirme yapar

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Rollerimizi Spring Security'nin anladığı formata çeviriyoruz
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.email; // Bizim sistemimizde "username" e-postadır
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Hesap süresi dolmuyor
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Hesap kilitlenmiyor
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Parola süresi dolmuyor
    }

    @Override
    public boolean isEnabled() {
        return true; // Hesap aktif
    }
}
