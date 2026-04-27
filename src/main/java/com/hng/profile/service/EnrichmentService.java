package com.hng.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hng.profile.dto.AgifyResponse;
import com.hng.profile.dto.GenderizeResponse;
import com.hng.profile.dto.NationalizeResponse;
import com.hng.profile.model.Profile;

@Service
public class EnrichmentService {

  private final RestTemplate restTemplate;

  public EnrichmentService() {
    this.restTemplate = new RestTemplate();
  }

  public Profile enrichName(String name) {
    GenderizeResponse genderData = restTemplate.getForObject(
        "https://api.genderize.io/?name=" + name, GenderizeResponse.class);

    if (genderData == null || genderData.gender() == null || genderData.count() == 0) {
      throw new RuntimeException("Genderize API returned an invalid response");
    }

    AgifyResponse ageData = restTemplate.getForObject(
        "https://api.agify.io/?name=" + name, AgifyResponse.class);

    if (ageData == null || ageData.age() == null) {
      throw new RuntimeException("Agify API returned an invalid response");
    }

    NationalizeResponse nationData = restTemplate.getForObject(
        "https://api.nationalize.io/?name=" + name, NationalizeResponse.class);

    if (nationData == null || nationData.country() == null || nationData.country().isEmpty()) {
      throw new RuntimeException("Nationalize API returned an invalid response");
    }

    String ageGroup = calculateAgeGroup(ageData.age());

    NationalizeResponse.CountryProbability topCountry = nationData.country().get(0);

    return new Profile(
        name.toLowerCase(),
        genderData.gender(),
        genderData.probability(),
        ageData.age(),
        ageGroup,
        topCountry.country_id(),
        null, // countryName not available from Nationalize API
        topCountry.probability());
  }

  private String calculateAgeGroup(int age) {
    if (age <= 12)
      return "child";
    if (age <= 19)
      return "teenager";
    if (age <= 59)
      return "adult";
    return "senior";
  }
}
