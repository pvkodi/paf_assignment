package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends com.sliitreserve.api.repositories.BaseRepository<User, UUID> {

    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByEmail(String email);
}
