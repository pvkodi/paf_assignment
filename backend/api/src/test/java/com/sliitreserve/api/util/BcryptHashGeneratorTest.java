package com.sliitreserve.api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.Test;

public class BcryptHashGeneratorTest {

    @Test
    public void generateHashForTestPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "YourPassword123!";
        
        String hash = encoder.encode(password);
        System.out.println("Generated Bcrypt Hash (strength 12):");
        System.out.println(hash);
        
        // Test verification
        boolean matches = encoder.matches(password, hash);
        System.out.println("\nVerification test: " + matches);
        System.out.println("Password matches hash: " + matches);
    }
}
