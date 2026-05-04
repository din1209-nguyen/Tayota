package com.tayota.userservice.object;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {
    private final UUID id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(com.tayota.userservice.entity.User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }


    @Override
    public String getUsername() {
        // Vì dự án không cần username nên thay bằng email
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override // Tài khoản không hết hạn
     public boolean isAccountNonExpired() {
         return true;
     }

     @Override // Tài khoản không bị khóa
     public boolean isAccountNonLocked() {
         return true;
     }

     @Override // Chứng chỉ không hết hạn
     public boolean isCredentialsNonExpired() {
         return true;
     }

     @Override // Tài khoản được kích hoạt
     public boolean isEnabled() {
         return true;
     }
}
