package com.fixitpro.security;

import com.fixitpro.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        // Despite the interface method's name, this resolves username,
        // email, OR phone - see UserRepository.findByUsernameOrEmailOrPhoneWithRole
        // for why this must stay in sync with AuthService.login()'s own lookup.
        return userRepository.findByUsernameOrEmailOrPhoneWithRole(identifier)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with identifier: " + identifier));
    }
}
