package com.hng.profile.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.hng.profile.model.Profile;
import com.hng.profile.parser.NaturalLanguageParser;
import com.hng.profile.service.ProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

  private final ProfileService profileService;
  private final NaturalLanguageParser languageParser;

  public ProfileController(ProfileService profileService, NaturalLanguageParser languageParser) {
    this.profileService = profileService;
    this.languageParser = languageParser;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> createProfile(@RequestBody Map<String, String> body) {
    String name = body.get("name");
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing or empty name");
    }

    ProfileService.ProfileCreationResult result = profileService.createOrGetProfile(name);

    if (result.alreadyExisted()) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(Map.of("status", "success", "message", "Profile already exists", "data", result.profile()));
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("status", "success", "data", result.profile()));
  }

  // Natural Language Search — must be declared BEFORE /{id} to avoid path
  // conflict
  @GetMapping("/search")
  public ResponseEntity<?> searchProfiles(
      @RequestParam String q,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int limit) {
    if (q == null || q.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing or empty parameter");
    }

    NaturalLanguageParser.ParsedQuery filters = languageParser.parse(q);

    Page<Profile> profilePage = profileService.getProfiles(
        filters.gender, filters.ageGroup, filters.countryId,
        filters.minAge, filters.maxAge, null, null,
        null, null, page, limit);

    PaginatedResponse response = new PaginatedResponse(
        "success", page, profilePage.getSize(),
        profilePage.getTotalElements(), profilePage.getContent());

    return ResponseEntity.ok(response);
  }

  // Advanced Filtering + Sorting + Pagination
  @GetMapping
  public ResponseEntity<?> getProfiles(
      @RequestParam(required = false) String gender,
      @RequestParam(required = false, name = "age_group") String ageGroup,
      @RequestParam(required = false, name = "country_id") String countryId,
      @RequestParam(required = false, name = "min_age") Integer minAge,
      @RequestParam(required = false, name = "max_age") Integer maxAge,
      @RequestParam(required = false, name = "min_gender_probability") Double minGenderProb,
      @RequestParam(required = false, name = "min_country_probability") Double minCountryProb,
      @RequestParam(required = false, name = "sort_by") String sortBy,
      @RequestParam(required = false) String order,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int limit) {

    Page<Profile> profilePage = profileService.getProfiles(
        gender, ageGroup, countryId, minAge, maxAge, minGenderProb, minCountryProb,
        sortBy, order, page, limit);

    PaginatedResponse response = new PaginatedResponse(
        "success", page, profilePage.getSize(),
        profilePage.getTotalElements(), profilePage.getContent());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getProfileById(@PathVariable UUID id) {
    Profile profile = profileService.getProfileById(id);
    return ResponseEntity.ok(Map.of("status", "success", "data", profile));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
    profileService.deleteProfile(id);
    return ResponseEntity.noContent().build();
  }
}

// Paginated response DTO
record PaginatedResponse(
    String status, int page, int limit, long total,
    List<Profile> data) {
}
