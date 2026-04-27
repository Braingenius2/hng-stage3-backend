package com.hng.profile.dto;

import java.util.List;

public record NationalizeResponse(
    String name,
    List<CountryProbability> country) {
  // Nested record to represent the items inside the array
  public record CountryProbability(
      String country_id,
      Double probability) {
  }
}
