package com.fixitpro.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Fetches the user together with its role in one query (JOIN FETCH).
     * Use this for anything that needs to read role/authorities after the
     * repository call returns (e.g. building a UserPrincipal for auth) -
     * User.role is LAZY, so a plain findByUsername() would otherwise throw
     * LazyInitializationException once the session closes.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    /**
     * Backs login-by-username-or-email-or-phone. A single query (rather than
     * three separate lookups) so this is one DB round trip regardless of
     * which identifier the person used, and one place to keep this rule
     * consistent - both CustomUserDetailsService (the actual Spring Security
     * authentication path) and AuthService.login()'s follow-up lookup call
     * this exact method, so a login via email can't authenticate
     * successfully and then fail on the next line because that line was
     * still looking up by exact username.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :identifier OR u.email = :identifier OR u.phone = :identifier")
    Optional<User> findByUsernameOrEmailOrPhoneWithRole(@Param("identifier") String identifier);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.userId")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.name = :roleName ORDER BY u.userId")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    long countByRole_Name(String roleName);
}
