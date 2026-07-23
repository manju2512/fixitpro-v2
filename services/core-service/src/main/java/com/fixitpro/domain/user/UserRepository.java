package com.fixitpro.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
