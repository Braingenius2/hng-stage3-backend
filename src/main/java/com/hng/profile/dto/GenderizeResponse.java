package com.hng.profile.dto;

public record GenderizeResponse(
    String name,
    String gender,
    Double probability,
    Integer count) {
}
