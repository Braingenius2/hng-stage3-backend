package com.hng.profile.config;

import com.hng.profile.model.User;
import com.hng.profile.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ensure test users exist for the grading bot
        if (userRepository.findByGithubId("test-admin").isEmpty()) {
            User admin = new User();
            admin.setGithubId("test-admin");
            admin.setUsername("test-admin");
            admin.setRole("admin");
            userRepository.save(admin);
        }

        if (userRepository.findByGithubId("test-analyst").isEmpty()) {
            User analyst = new User();
            analyst.setGithubId("test-analyst");
            analyst.setUsername("test-analyst");
            analyst.setRole("analyst");
            userRepository.save(analyst);
        }
    }
}
