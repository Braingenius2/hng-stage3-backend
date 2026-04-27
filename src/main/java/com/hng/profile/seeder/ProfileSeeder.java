package com.hng.profile.seeder;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.profile.model.Profile;
import com.hng.profile.repository.ProfileRepository;

@Component
public class ProfileSeeder implements CommandLineRunner {

  private final ProfileRepository profileRepository;
  private final ObjectMapper objectMapper;

  public ProfileSeeder(ProfileRepository profileRepository, ObjectMapper objectMapper) {
    this.profileRepository = profileRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(String... args) throws Exception {
    System.out.println("Seeding database with profiles from seed file...");

    InputStream inputStream = getClass().getResourceAsStream("/seed_profiles.json");
    if (inputStream == null) {
      throw new IllegalStateException("seed_profiles.json not found");
    }

    SeedWrapper wrapper = objectMapper.readValue(inputStream, SeedWrapper.class);

    Set<String> seedNamesLower = wrapper.profiles().stream()
        .map(dto -> dto.name().trim().toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());

    Set<String> existingNamesLower = profileRepository.findExistingLowerCaseNames(seedNamesLower);

    List<Profile> profilesToSave = wrapper.profiles().stream()
        .filter(dto -> !existingNamesLower.contains(dto.name().trim().toLowerCase(Locale.ROOT)))
        .map(dto -> new Profile(
            dto.name().trim(),
            dto.gender(),
            dto.gender_probability(),
            dto.age(),
            dto.age_group(),
            dto.country_id(),
            dto.country_name(),
            dto.country_probability()))
        .toList();

    if (profilesToSave.isEmpty()) {
      System.out.println("Database is already seeded.");
      return;
    }

    profileRepository.saveAll(profilesToSave);
    System.out.println("Database seeding complete! Added " + profilesToSave.size() + " new profiles.");
  }

  public record SeedWrapper(List<SeedDTO> profiles) {
  }

  public record SeedDTO(
      String name, String gender, Double gender_probability,
      Integer age, String age_group, String country_id,
      String country_name, Double country_probability) {
  }
}
