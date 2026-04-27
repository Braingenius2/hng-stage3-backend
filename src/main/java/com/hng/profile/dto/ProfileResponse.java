package com.hng.profile.dto;

import com.hng.profile.model.Profile;

public record ProfileResponse(String status, String message, Profile data) {
}
