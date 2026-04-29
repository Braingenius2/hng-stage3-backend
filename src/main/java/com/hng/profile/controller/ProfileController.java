package com.hng.profile.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

    Map<String, String> links = new HashMap<>();
    links.put("self", "/api/profiles/search?q=" + q + "&page=" + page + "&limit=" + limit);
    if (page < profilePage.getTotalPages()) {
      links.put("next", "/api/profiles/search?q=" + q + "&page=" + (page + 1) + "&limit=" + limit);
    }

    PaginationMeta meta = new PaginationMeta(
        page, limit, profilePage.getTotalElements(), profilePage.getTotalPages(), links);
    PaginatedResponse response = new PaginatedResponse(profilePage.getContent(), meta);

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

    Map<String, String> links = new HashMap<>();
    links.put("self", "/api/profiles?page=" + page + "&limit=" + limit);
    if (page < profilePage.getTotalPages()) {
      links.put("next", "/api/profiles?page=" + (page + 1) + "&limit=" + limit);
    }

    PaginationMeta meta = new PaginationMeta(
        page, limit, profilePage.getTotalElements(), profilePage.getTotalPages(), links);
    PaginatedResponse response = new PaginatedResponse(profilePage.getContent(), meta);

    return ResponseEntity.ok(response);

  }

  @GetMapping("/export")
  public void exportProfiles(
      @RequestParam(required = false) String gender,
      @RequestParam(required = false, name = "age_group") String ageGroup,
      @RequestParam(required = false, name = "country_id") String countryId,
      @RequestParam(required = false, name = "min_age") Integer minAge,
      @RequestParam(required = false, name = "max_age") Integer maxAge,
      @RequestParam(required = false, name = "min_gender_probability") Double minGenderProb,
      @RequestParam(required = false, name = "min_country_probability") Double minCountryProb,
      @RequestParam(required = false, name = "sort_by") String sortBy,
      @RequestParam(required = false) String order,
      HttpServletResponse response) throws IOException {

    // Fetch all profiles matching the filters (using MAX_VALUE for page size)
    Page<Profile> profilePage = profileService.getProfiles(
        gender, ageGroup, countryId, minAge, maxAge, minGenderProb, minCountryProb,
        sortBy, order, 1, Integer.MAX_VALUE);

    // Set the required headers for CSV download
    response.setContentType("text/csv");
    response.setHeader("Content-Disposition", "attachment; filename=\"profiles.csv\"");

    // Write the data to the response using Commons CSV
    try (PrintWriter writer = response.getWriter();
         CSVPrinter csvPrinter = new CSVPrinter(writer, 
            CSVFormat.DEFAULT.builder().setHeader(
                "id", "name", "gender", "gender_probability", "age", "age_group",
                "country_id", "country_name", "country_probability", "created_at"
            ).build())) {
         
        for (Profile profile : profilePage.getContent()) {
            csvPrinter.printRecord(
                profile.getId(),
                profile.getName(),
                profile.getGender(),
                profile.getGenderProbability(),
                profile.getAge(),
                profile.getAgeGroup(),
                profile.getCountryId(),
                profile.getCountryName(),
                profile.getCountryProbability(),
                profile.getCreatedAt()
            );
        }
    }
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

// Enterprise Paginated Response DTOs
record PaginatedResponse(
    List<Profile> data,
    PaginationMeta meta) {
}

record PaginationMeta(
    int page,
    int size,
    long total_elements,
    int total_pages,
    Map<String, String> links) {
}
