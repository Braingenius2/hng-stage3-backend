package com.hng.profile.specification;

import org.springframework.data.jpa.domain.Specification;
import com.hng.profile.model.Profile;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class ProfileSpecification {

  public static Specification<Profile> buildFilter(
      String gender, String ageGroup, String countryId,
      Integer minAge, Integer maxAge,
      Double minGenderProb, Double minCountryProb) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (gender != null && !gender.isBlank()) {
        predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("gender")), gender.toLowerCase()));
      }
      if (ageGroup != null && !ageGroup.isBlank()) {
        predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("ageGroup")), ageGroup.toLowerCase()));
      }
      if (countryId != null && !countryId.isBlank()) {
        predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("countryId")), countryId.toUpperCase()));
      }

      if (minAge != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("age"), minAge));
      }
      if (maxAge != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("age"), maxAge));
      }

      if (minGenderProb != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("genderProbability"), minGenderProb));
      }
      if (minCountryProb != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("countryProbability"), minCountryProb));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
