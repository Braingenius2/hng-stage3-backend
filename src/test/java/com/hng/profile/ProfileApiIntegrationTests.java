package com.hng.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.profile.repository.ProfileRepository;
import com.hng.profile.seeder.ProfileSeeder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ProfileApiIntegrationTests.TestErrorController.class)
class ProfileApiIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProfileSeeder profileSeeder;

  @Autowired
  private ProfileRepository profileRepository;

  @Test
  void getProfiles_appliesAllSupportedFiltersAndUsesExactResponseShape() throws Exception {
    JsonNode genderNode = getOkJson("/api/profiles", Map.of("gender", "male", "limit", "50"));
    assertThat(genderNode.get("status").asText()).isEqualTo("success");
    for (JsonNode item : genderNode.get("data")) {
      assertThat(item.get("gender").asText()).isEqualTo("male");
    }

    JsonNode ageGroupNode = getOkJson("/api/profiles", Map.of("age_group", "adult", "limit", "50"));
    for (JsonNode item : ageGroupNode.get("data")) {
      assertThat(item.get("age_group").asText()).isEqualTo("adult");
    }

    JsonNode countryNode = getOkJson("/api/profiles", Map.of("country_id", "NG", "limit", "50"));
    for (JsonNode item : countryNode.get("data")) {
      assertThat(item.get("country_id").asText()).isEqualTo("NG");
      assertThat(item.get("country_id").asText()).hasSize(2);
    }

    JsonNode ageRangeNode = getOkJson("/api/profiles", Map.of("min_age", "20", "max_age", "30", "limit", "50"));
    for (JsonNode item : ageRangeNode.get("data")) {
      assertThat(item.get("age").asInt()).isBetween(20, 30);
    }

    JsonNode probNode = getOkJson("/api/profiles",
        Map.of("min_gender_probability", "0.7", "min_country_probability", "0.5", "limit", "50"));
    for (JsonNode item : probNode.get("data")) {
      assertThat(item.get("gender_probability").asDouble()).isGreaterThanOrEqualTo(0.7);
      assertThat(item.get("country_probability").asDouble()).isGreaterThanOrEqualTo(0.5);
    }

    JsonNode defaultNode = getOkJson("/api/profiles", Map.of());
    assertThat(defaultNode.get("page").asInt()).isEqualTo(1);
    assertThat(defaultNode.get("limit").asInt()).isEqualTo(10);
    assertThat(defaultNode.get("total").asInt()).isEqualTo(2026);
    assertThat(defaultNode.get("data")).hasSize(10);

    JsonNode first = defaultNode.get("data").get(0);
    assertThat(first.has("sample_size")).isFalse();
  }

  @Test
  void getProfiles_combinedFiltersUseAndLogic() throws Exception {
    JsonNode node = getOkJson("/api/profiles",
        Map.of("gender", "male", "country_id", "NG", "min_age", "25", "limit", "50"));

    assertThat(node.get("data").size()).isGreaterThan(0);
    for (JsonNode item : node.get("data")) {
      assertThat(item.get("gender").asText()).isEqualTo("male");
      assertThat(item.get("country_id").asText()).isEqualTo("NG");
      assertThat(item.get("age").asInt()).isGreaterThanOrEqualTo(25);
    }
  }

  @Test
  void getProfiles_supportsSortingAndPaginationRules() throws Exception {
    JsonNode ageDesc = getOkJson("/api/profiles", Map.of("sort_by", "age", "order", "desc", "limit", "50"));
    assertMonotonic(ageDesc, "age", true);

    JsonNode createdDesc = getOkJson("/api/profiles",
        Map.of("sort_by", "created_at", "order", "desc", "limit", "50"));
    assertMonotonic(createdDesc, "created_at", true);

    JsonNode genderProbAsc = getOkJson("/api/profiles",
        Map.of("sort_by", "gender_probability", "order", "asc", "limit", "50"));
    assertMonotonic(genderProbAsc, "gender_probability", false);

    JsonNode pageTwo = getOkJson("/api/profiles", Map.of("page", "2", "limit", "7"));
    assertThat(pageTwo.get("page").asInt()).isEqualTo(2);
    assertThat(pageTwo.get("limit").asInt()).isEqualTo(7);
    assertThat(pageTwo.get("data").size()).isLessThanOrEqualTo(7);

    JsonNode cappedLimit = getOkJson("/api/profiles", Map.of("limit", "100"));
    assertThat(cappedLimit.get("limit").asInt()).isEqualTo(50);
    assertThat(cappedLimit.get("data").size()).isEqualTo(50);
  }

  @Test
  void getProfiles_rejectsInvalidQueryParametersWith400() throws Exception {
    assertError("/api/profiles?sort_by=height", 400, "Invalid query parameters");
    assertError("/api/profiles?order=desc", 400, "Invalid query parameters");
    assertError("/api/profiles?gender=unknown", 400, "Invalid query parameters");
    assertError("/api/profiles?age_group=elder", 400, "Invalid query parameters");
    assertError("/api/profiles?country_id=NGA", 400, "Invalid query parameters");
    assertError("/api/profiles?min_age=40&max_age=20", 400, "Invalid query parameters");
    assertError("/api/profiles?page=0", 400, "Invalid query parameters");
    assertError("/api/profiles?limit=0", 400, "Invalid query parameters");
  }

  @Test
  void naturalLanguageSearch_supportsRequiredMappingsAndPagination() throws Exception {
    JsonNode youngMales = getOkJson("/api/profiles/search", Map.of("q", "young males", "limit", "50"));
    for (JsonNode item : youngMales.get("data")) {
      assertThat(item.get("gender").asText()).isEqualTo("male");
      assertThat(item.get("age").asInt()).isBetween(16, 24);
    }

    JsonNode femalesAbove30 = getOkJson("/api/profiles/search", Map.of("q", "females above 30", "limit", "50"));
    for (JsonNode item : femalesAbove30.get("data")) {
      assertThat(item.get("gender").asText()).isEqualTo("female");
      assertThat(item.get("age").asInt()).isGreaterThanOrEqualTo(30);
    }

    JsonNode fromAngola = getOkJson("/api/profiles/search", Map.of("q", "people from angola", "limit", "50"));
    for (JsonNode item : fromAngola.get("data")) {
      assertThat(item.get("country_id").asText()).isEqualTo("AO");
    }

    JsonNode adultMalesKenya = getOkJson("/api/profiles/search", Map.of("q", "adult males from kenya", "limit", "50"));
    for (JsonNode item : adultMalesKenya.get("data")) {
      assertThat(item.get("gender").asText()).isEqualTo("male");
      assertThat(item.get("age_group").asText()).isEqualTo("adult");
      assertThat(item.get("country_id").asText()).isEqualTo("KE");
    }

    JsonNode teenMixed = getOkJson("/api/profiles/search",
        Map.of("q", "male and female teenagers above 17", "limit", "50"));
    JsonNode baseline = getOkJson("/api/profiles", Map.of("age_group", "teenager", "min_age", "17", "limit", "50"));
    assertThat(teenMixed.get("total").asInt()).isEqualTo(baseline.get("total").asInt());

    JsonNode searchDefault = getOkJson("/api/profiles/search", Map.of("q", "young males"));
    assertThat(searchDefault.get("page").asInt()).isEqualTo(1);
    assertThat(searchDefault.get("limit").asInt()).isEqualTo(10);

    JsonNode searchMax = getOkJson("/api/profiles/search", Map.of("q", "young males", "limit", "100"));
    assertThat(searchMax.get("limit").asInt()).isEqualTo(50);
  }

  @Test
  void queryValidationAndErrorContracts_matchRequiredStatuses() throws Exception {
    assertError("/api/profiles/search", 400, "Missing or empty parameter");
    assertError("/api/profiles/search?q=", 400, "Missing or empty parameter");
    assertError("/api/profiles/search?q=abracadabra%20qwerty", 400, "Unable to interpret query");

    assertError("/api/profiles?min_age=abc", 422, "Invalid parameter type");
    assertError("/api/profiles?page=abc", 422, "Invalid parameter type");

    assertError("/api/profiles/" + UUID.randomUUID(), 404, "Profile not found");
    assertError("/api/test/error", 500, "Server failure");
  }

  @Test
  void startupDataAndSeederBehavior_areCompliant() throws Exception {
    long before = profileRepository.count();
    assertThat(before).isEqualTo(2026);

    profileSeeder.run();

    long after = profileRepository.count();
    assertThat(after).isEqualTo(before);
    long distinctNames = profileRepository.findAll().stream().map(p -> p.getName().toLowerCase()).distinct().count();
    assertThat(distinctNames).isEqualTo(after);

    String firstId = profileRepository.findAll().get(0).getId().toString();
    assertThat(firstId.charAt(14)).isEqualTo('7');
  }

  @Test
  void corsAndTimestampFormat_areCompliant() throws Exception {
    MvcResult corsResult = mockMvc.perform(
        get("/api/profiles").param("page", "1").param("limit", "1").header("Origin", "https://example.com"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "*"))
        .andReturn();

    JsonNode body = objectMapper.readTree(corsResult.getResponse().getContentAsString());
    String createdAt = body.get("data").get(0).get("created_at").asText();
    assertThat(Pattern.matches("\\d{4}-\\d{2}-\\d{2}T.*Z", createdAt)).isTrue();
  }

  private JsonNode getOkJson(String path, Map<String, String> params) throws Exception {
    MockHttpServletRequestBuilder request = get(path).accept(MediaType.APPLICATION_JSON);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      request.param(entry.getKey(), entry.getValue());
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private void assertError(String path, int statusCode, String message) throws Exception {
    MvcResult result = mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(statusCode))
        .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(node.get("status").asText()).isEqualTo("error");
    assertThat(node.get("message").asText()).isEqualTo(message);
  }

  private void assertMonotonic(JsonNode responseNode, String field, boolean descending) {
    if (responseNode.get("data").size() < 2) {
      return;
    }

    JsonNode previous = responseNode.get("data").get(0).get(field);
    for (int i = 1; i < responseNode.get("data").size(); i++) {
      JsonNode current = responseNode.get("data").get(i).get(field);

      int comparison;
      if (previous.isNumber()) {
        comparison = Double.compare(previous.asDouble(), current.asDouble());
      } else {
        comparison = previous.asText().compareTo(current.asText());
      }

      if (descending) {
        assertThat(comparison).isGreaterThanOrEqualTo(0);
      } else {
        assertThat(comparison).isLessThanOrEqualTo(0);
      }
      previous = current;
    }
  }

  @RestController
  static class TestErrorController {
    @GetMapping("/api/test/error")
    String failAlways() {
      throw new RuntimeException("forced error");
    }
  }
}
