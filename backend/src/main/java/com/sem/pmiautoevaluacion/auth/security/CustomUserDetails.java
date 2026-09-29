package com.sem.pmiautoevaluacion.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sem.pmiautoevaluacion.users.entity.Establishment;
import com.sem.pmiautoevaluacion.users.entity.Secretary;
import com.sem.pmiautoevaluacion.users.entity.User;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;
    
    public CustomUserDetails(User user){
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override 
    public String getUsername(){
        if(user instanceof Establishment establishment) {
            return establishment.getDaneCode();
        }
        return user.getEmail().getValue();
    }

    @Override 
    public String getPassword() {
        return user.getPassword();
    }

    @Override 
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = (user instanceof Secretary) ? "SECRETARY" : "ESTABLISHMENT";
        return List.of(new SimpleGrantedAuthority("ROLE_"+role));
    }

    @Override 
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override 
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override 
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override 
    public boolean isEnabled() {
        return true;
    }

}
