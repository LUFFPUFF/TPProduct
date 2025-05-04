package com.example.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Objects;

@Getter
@Setter
public class AppUserDetails extends User {

    private final Integer id;
    private final Integer companyId;

    public AppUserDetails(Integer id,
                          Integer companyId,
                          String username,
                          String password,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.companyId = companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AppUserDetails that = (AppUserDetails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    @Override
    public String toString() {
        return "AppUserDetails{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", username='" + getUsername() + '\'' +
                ", authorities=" + getAuthorities() +
                '}';
    }
}
