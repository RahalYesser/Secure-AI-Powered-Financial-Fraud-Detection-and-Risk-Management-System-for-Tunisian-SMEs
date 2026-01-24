package com.tunisia.financial.seeder;

import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data seeder for creating fake users on application startup
 * Only runs if the users table is empty
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // Check if users table is empty
            if (userRepository.count() == 0) {
                log.info("Users table is empty. Starting data seeding...");
                seedUsers();
                log.info("Data seeding completed successfully!");
            } else {
                log.info("Users table already contains data. Skipping seeding.");
            }
        } catch (Exception e) {
            log.warn("Data seeding skipped - tables may not exist yet: {}", e.getMessage());
        }
    }

    private void seedUsers() {
        List<User> users = new ArrayList<>();

        // Seed ADMIN users (10 users)
        for (int i = 1; i <= 10; i++) {
            User admin = createUser(
                    "admin" + i + "@financial.tn",
                    "Admin",
                    "User" + i,
                    getAdminPassword(i),
                    UserRole.ADMIN
            );
            users.add(admin);
        }

        // Seed FINANCIAL_ANALYST users (8 users)
        for (int i = 1; i <= 8; i++) {
            User analyst = createUser(
                    "analyst" + i + "@financial.tn",
                    "Analyst",
                    "User" + i,
                    getAnalystPassword(i),
                    UserRole.FINANCIAL_ANALYST
            );
            users.add(analyst);
        }

        // Seed SME_USER users (7 users)
        for (int i = 1; i <= 7; i++) {
            User smeUser = createUser(
                    "sme" + i + "@financial.tn",
                    "SME",
                    "User" + i,
                    getSmePassword(i),
                    UserRole.SME_USER
            );
            users.add(smeUser);
        }

        // Seed AUDITOR users (5 users)
        for (int i = 1; i <= 5; i++) {
            User auditor = createUser(
                    "auditor" + i + "@financial.tn",
                    "Auditor",
                    "User" + i,
                    getAuditorPassword(i),
                    UserRole.AUDITOR
            );
            users.add(auditor);
        }

        // Save all users to database
        userRepository.saveAll(users);

        log.info("Created {} users:", users.size());
        log.info("  - 10 ADMIN users (admin1@financial.tn to admin10@financial.tn)");
        log.info("  - 8 FINANCIAL_ANALYST users (analyst1@financial.tn to analyst8@financial.tn)");
        log.info("  - 7 SME_USER users (sme1@financial.tn to sme7@financial.tn)");
        log.info("  - 5 AUDITOR users (auditor1@financial.tn to auditor5@financial.tn)");
        log.info("");
        log.info("Password patterns:");
        log.info("  - Admins: Admin123!, Admin123@, Admin123#, etc.");
        log.info("  - Analysts: Analyst123!, Analyst123@, Analyst123#, etc.");
        log.info("  - SME Users: Sme123!, Sme123@, Sme123#, etc.");
        log.info("  - Auditors: Auditor123!, Auditor123@, Auditor123#, etc.");
    }

    private User createUser(String email, String firstName, String lastName, String password, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    /**
     * Generate password for admin users with pattern: Admin123!, Admin123@, etc.
     */
    private String getAdminPassword(int index) {
        char[] specialChars = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'};
        char special = specialChars[(index - 1) % specialChars.length];
        return "Admin123" + special;
    }

    /**
     * Generate password for analyst users with pattern: Analyst123!, Analyst123@, etc.
     */
    private String getAnalystPassword(int index) {
        char[] specialChars = {'!', '@', '#', '$', '%', '^', '&', '*'};
        char special = specialChars[(index - 1) % specialChars.length];
        return "Analyst123" + special;
    }

    /**
     * Generate password for SME users with pattern: Sme123!, Sme123@, etc.
     */
    private String getSmePassword(int index) {
        char[] specialChars = {'!', '@', '#', '$', '%', '^', '&'};
        char special = specialChars[(index - 1) % specialChars.length];
        return "Sme123" + special;
    }

    /**
     * Generate password for auditor users with pattern: Auditor123!, Auditor123@, etc.
     */
    private String getAuditorPassword(int index) {
        char[] specialChars = {'!', '@', '#', '$', '%'};
        char special = specialChars[(index - 1) % specialChars.length];
        return "Auditor123" + special;
    }
}
