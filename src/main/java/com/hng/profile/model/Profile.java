package com.hng.profile.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.github.f4b6a3.uuid.UuidCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "profiles")
public class Profile {

  @Id
  private UUID id;

  @Column(unique = true, nullable = false)
  private String name;

  private String gender;

  @Column(name = "gender_probability")
  private Double genderProbability;

  private Integer age;

  @Column(name = "age_group")
  private String ageGroup;

  @Column(name = "country_id", length = 2)
  private String countryId;

  @Column(name = "country_name")
  private String countryName;

  @Column(name = "country_probability")
  private Double countryProbability;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Profile() {

  }

  public Profile(String name, String gender, Double genderProbability,
      Integer age, String ageGroup, String countryId, String countryName, Double countryProbability) {
    this.id = UuidCreator.getTimeOrderedEpoch(); // Generates UUID v7!
    this.name = name;
    this.gender = gender;
    this.genderProbability = genderProbability;
    this.age = age;
    this.ageGroup = ageGroup;
    this.countryId = countryId;
    this.countryName = countryName;
    this.countryProbability = countryProbability;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getGender() {
    return gender;
  }

  @JsonProperty("gender_probability")
  public Double getGenderProbability() {
    return genderProbability;
  }

  public Integer getAge() {
    return age;
  }

  @JsonProperty("age_group")
  public String getAgeGroup() {
    return ageGroup;
  }

  @JsonProperty("country_id")
  public String getCountryId() {
    return countryId;
  }

  @JsonProperty("country_name")
  public String getCountryName() {
    return countryName;
  }

  @JsonProperty("country_probability")
  public Double getCountryProbability() {
    return countryProbability;
  }

  @JsonProperty("created_at")
  @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public void setCountryId(String countryId) {
    this.countryId = countryId;
  }

  public void setCountryName(String countryName) {
    this.countryName = countryName;
  }

  public void setAgeGroup(String ageGroup) {
    this.ageGroup = ageGroup;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setGenderProbability(Double genderProbability) {
    this.genderProbability = genderProbability;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public void setCountryProbability(Double countryProbability) {
    this.countryProbability = countryProbability;
  }

}
