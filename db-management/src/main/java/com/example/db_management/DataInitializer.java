package com.example.db_management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.init.password:password123}")
    private String initPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(build("admin",  "admin@example.com",  "ADMIN",  initPassword));
            userRepository.save(build("editor", "editor@example.com", "EDITOR", initPassword));
            userRepository.save(build("viewer", "viewer@example.com", "VIEWER", initPassword));
        }
    }

    private User build(String username, String email, String role, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return user;
    }
}
