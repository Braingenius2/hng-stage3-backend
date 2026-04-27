package com.hng.profile.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.hng.profile.exception.ResourceNotFoundException;
import com.hng.profile.model.Profile;
import com.hng.profile.repository.ProfileRepository;
import com.hng.profile.specification.ProfileSpecification;

@Service
public class ProfileService {
  private static final Set<String> ALLOWED_GENDERS = Set.of("male", "female");
  private static final Set<String> ALLOWED_AGE_GROUPS = Set.of("child", "teenager", "adult", "senior");
  private static final Set<String> ALLOWED_SORT_BY = Set.of("age", "created_at", "gender_probability", "country_probability");
  private static final Set<String> ALLOWED_ORDER = Set.of("asc", "desc");

  private final ProfileRepository profileRepository;
  private final EnrichmentService enrichmentService;

  public ProfileService(ProfileRepository profileRepository, EnrichmentService enrichmentService) {
    this.profileRepository = profileRepository;
    this.enrichmentService = enrichmentService;
  }

  public ProfileCreationResult createOrGetProfile(String name) {
    String cleanName = name.trim().toLowerCase();
    Optional<Profile> existing = profileRepository.findByNameIgnoreCase(cleanName);
    if (existing.isPresent()) {
      return new ProfileCreationResult(existing.get(), true);
    }

    Profile newProfile = enrichmentService.enrichName(cleanName);
    Profile savedProfile = profileRepository.save(newProfile);

    return new ProfileCreationResult(savedProfile, false);
  }

  public Profile getProfileById(UUID id) {
    return profileRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
  }

  public Page<Profile> getProfiles(
      String gender, String ageGroup, String countryId,
      Integer minAge, Integer maxAge,
      Double minGenderProb, Double minCountryProb,
      String sortBy, String order,
      int page, int limit) {
    validateQueryParameters(
        gender, ageGroup, countryId, minAge, maxAge, minGenderProb, minCountryProb, sortBy, order, page, limit);

    // 1. Handle Sorting
    Sort sort = Sort.unsorted();
    if (sortBy != null && !sortBy.isBlank()) {
      Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
      String sortProperty = switch (sortBy.toLowerCase()) {
        case "created_at" -> "createdAt";
        case "gender_probability" -> "genderProbability";
        case "country_probability" -> "countryProbability";
        default -> sortBy; // pass through "age" etc.
      };
      sort = Sort.by(direction, sortProperty);
    }

    // 2. Handle Pagination (Spring is 0-indexed, API expects 1-indexed)
    int springPage = page - 1;
    int safeLimit = Math.min(limit, 50);
    Pageable pageable = PageRequest.of(springPage, safeLimit, sort);

    // 3. Build the Filter Specification
    Specification<Profile> spec = ProfileSpecification.buildFilter(
        gender, ageGroup, countryId, minAge, maxAge, minGenderProb, minCountryProb);

    // 4. Query the Database
    return profileRepository.findAll(spec, pageable);
  }

  public void deleteProfile(UUID id) {
    if (!profileRepository.existsById(id)) {
      throw new ResourceNotFoundException("Profile not found");
    }
    profileRepository.deleteById(id);
  }

  public record ProfileCreationResult(Profile profile, boolean alreadyExisted) {
  }

  private void validateQueryParameters(
      String gender, String ageGroup, String countryId,
      Integer minAge, Integer maxAge,
      Double minGenderProb, Double minCountryProb,
      String sortBy, String order,
      int page, int limit) {
    if (gender != null && !ALLOWED_GENDERS.contains(gender.toLowerCase())) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (ageGroup != null && !ALLOWED_AGE_GROUPS.contains(ageGroup.toLowerCase())) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (countryId != null && !countryId.matches("^[A-Za-z]{2}$")) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (minAge != null && minAge < 0) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (maxAge != null && maxAge < 0) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (minAge != null && maxAge != null && minAge > maxAge) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (minGenderProb != null && (minGenderProb < 0 || minGenderProb > 1)) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (minCountryProb != null && (minCountryProb < 0 || minCountryProb > 1)) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (page < 1 || limit < 1) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (sortBy != null && !sortBy.isBlank() && !ALLOWED_SORT_BY.contains(sortBy.toLowerCase())) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (order != null && !order.isBlank() && !ALLOWED_ORDER.contains(order.toLowerCase())) {
      throw new IllegalArgumentException("Invalid query parameters");
    }

    if (order != null && !order.isBlank() && (sortBy == null || sortBy.isBlank())) {
      throw new IllegalArgumentException("Invalid query parameters");
    }
  }
}
